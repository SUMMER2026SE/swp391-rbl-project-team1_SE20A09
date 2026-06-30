package com.sportvenue.repository.specification;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.StadiumComplex;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class StadiumSpecification {

    public static Specification<Stadium> withDynamicFilter(StadiumSearchRequest req, boolean isPublicSearch) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // 0. NodeType Filter - Chỉ tìm kiếm COURT (sân lẻ bookable)
            predicates.add(cb.equal(root.get("nodeType"), StadiumNodeType.COURT));

            // 1. Status Filter
            if (isPublicSearch) {
                predicates.add(cb.equal(root.get("stadiumStatus"), StadiumStatus.AVAILABLE));
                // Thừa hưởng phê duyệt: Sân cũ approved_status = APPROVED HOẶC Complex cha approved_status = APPROVED
                predicates.add(cb.or(
                    cb.equal(root.get("approvedStatus"), ApprovedStatus.APPROVED),
                    cb.equal(root.get("parentStadium").get("complex").get("approvedStatus"), ApprovedStatus.APPROVED)
                ));
            } else if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("stadiumStatus"), req.getStatus()));
            }

            addKeywordPredicate(predicates, cb, root, req.getKeyword());
            addBasicFilters(predicates, cb, root, query, req);
            addTimeSlotPredicate(predicates, cb, root, query, req);
            addLocationPredicate(predicates, cb, root, req);
            addAmenitiesPredicate(predicates, cb, root, query, req.getAmenityIds());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addKeywordPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, String kw) {
        if (StringUtils.hasText(kw)) {
            String likePattern = "%" + kw.toLowerCase() + "%";
            Predicate nameLike = cb.like(cb.lower(root.get("stadiumName")), likePattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
            preds.add(cb.or(nameLike, descLike));
        }
    }

    private static void addBasicFilters(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, CriteriaQuery<?> query, StadiumSearchRequest req) {
        if (req.getSportTypeId() != null) {
            preds.add(cb.or(
                cb.equal(root.get("sportType").get("sportTypeId"), req.getSportTypeId()),
                cb.equal(root.get("parentStadium").get("sportType").get("sportTypeId"), req.getSportTypeId())
            ));
        }
        if (StringUtils.hasText(req.getAddress())) {
            String searchAddress = "%" + req.getAddress().toLowerCase() + "%";
            preds.add(cb.or(
                cb.like(cb.lower(root.get("address")), searchAddress),
                cb.like(cb.lower(root.get("parentStadium").get("complex").get("address")), searchAddress)
            ));
        }
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            Subquery<Integer> priceSubquery = query.subquery(Integer.class);
            Root<TimeSlot> priceSlotRoot = priceSubquery.from(TimeSlot.class);
            priceSubquery.select(priceSlotRoot.get("stadium").get("stadiumId"));
            
            List<Predicate> pricePreds = new ArrayList<>();
            pricePreds.add(cb.equal(priceSlotRoot.get("stadium").get("stadiumId"), root.get("stadiumId")));
            
            if (req.getMinPrice() != null) {
                pricePreds.add(cb.greaterThanOrEqualTo(priceSlotRoot.get("pricePerSlot"), req.getMinPrice()));
            }
            if (req.getMaxPrice() != null) {
                pricePreds.add(cb.lessThanOrEqualTo(priceSlotRoot.get("pricePerSlot"), req.getMaxPrice()));
            }
            
            priceSubquery.where(cb.and(pricePreds.toArray(new Predicate[0])));
            preds.add(cb.exists(priceSubquery));
        }
    }

    private static void addTimeSlotPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, CriteriaQuery<?> query, StadiumSearchRequest req) {
        if (req.getStartTime() != null && req.getEndTime() != null) {
            Subquery<Integer> slotSubquery = query.subquery(Integer.class);
            var slotRoot = slotSubquery.from(TimeSlot.class);
            slotSubquery.select(slotRoot.get("stadium").get("stadiumId"));
            
            Predicate stadiumMatch = cb.equal(slotRoot.get("stadium").get("stadiumId"), root.get("stadiumId"));
            Predicate statusMatch = cb.equal(slotRoot.get("slotStatus"), SlotStatus.AVAILABLE);
            Predicate timeMatch = cb.and(
                    cb.lessThan(slotRoot.get("startTime"), req.getEndTime()),
                    cb.greaterThan(slotRoot.get("endTime"), req.getStartTime())
            );
            
            slotSubquery.where(cb.and(stadiumMatch, statusMatch, timeMatch));
            preds.add(cb.exists(slotSubquery));
        }
    }

    private static void addLocationPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, StadiumSearchRequest req) {
        if (req.getUserLat() != null && req.getUserLng() != null && req.getRadiusInKm() != null) {
            double latDelta = req.getRadiusInKm() / 111.0;
            double lonDelta = req.getRadiusInKm() / (111.0 * Math.cos(Math.toRadians(req.getUserLat())));

            double minLat = req.getUserLat() - latDelta;
            double maxLat = req.getUserLat() + latDelta;
            double minLon = req.getUserLng() - lonDelta;
            double maxLon = req.getUserLng() + lonDelta;

            preds.add(cb.or(
                cb.between(root.get("latitude"), minLat, maxLat),
                cb.between(root.get("parentStadium").get("complex").get("latitude"), minLat, maxLat)
            ));
            preds.add(cb.or(
                cb.between(root.get("longitude"), minLon, maxLon),
                cb.between(root.get("parentStadium").get("complex").get("longitude"), minLon, maxLon)
            ));
        }
    }

    private static void addAmenitiesPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, CriteriaQuery<?> query, List<Integer> amenityIds) {
        if (amenityIds != null && !amenityIds.isEmpty()) {
            // 1. Kiểm tra tiện nghi trên COURT trực tiếp (Dữ liệu cũ hoặc override)
            Subquery<Long> courtAmenitySubquery = query.subquery(Long.class);
            var courtSubRoot = courtAmenitySubquery.from(Stadium.class);
            Join<Stadium, Amenity> courtAmenityJoin = courtSubRoot.join("amenities");
            courtAmenitySubquery.select(cb.countDistinct(courtAmenityJoin.get("amenityId")));
            courtAmenitySubquery.where(cb.and(
                cb.equal(courtSubRoot.get("stadiumId"), root.get("stadiumId")),
                courtAmenityJoin.get("amenityId").in(amenityIds)
            ));

            // 2. Kiểm tra tiện nghi trên Complex sở hữu (Cấu trúc 3 tầng mới)
            Subquery<Long> complexAmenitySubquery = query.subquery(Long.class);
            var complexSubRoot = complexAmenitySubquery.from(StadiumComplex.class);
            Join<StadiumComplex, Amenity> complexAmenityJoin = complexSubRoot.join("amenities");
            complexAmenitySubquery.select(cb.countDistinct(complexAmenityJoin.get("amenityId")));
            complexAmenitySubquery.where(cb.and(
                cb.equal(complexSubRoot.get("complexId"), root.get("parentStadium").get("complex").get("complexId")),
                complexAmenityJoin.get("amenityId").in(amenityIds)
            ));

            preds.add(cb.or(
                cb.equal(courtAmenitySubquery, (long) amenityIds.size()),
                cb.equal(complexAmenitySubquery, (long) amenityIds.size())
            ));
        }
    }
}
