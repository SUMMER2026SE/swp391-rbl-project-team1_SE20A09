package com.sportvenue.repository.specification;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StadiumSpecification {

    public static Specification<Stadium> withDynamicFilter(StadiumSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Only AVAILABLE stadiums
            predicates.add(cb.equal(root.get("stadiumStatus"), StadiumStatus.AVAILABLE));

            addKeywordPredicate(predicates, cb, root, req.getKeyword());
            addBasicFilters(predicates, cb, root, req);
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

    private static void addBasicFilters(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, StadiumSearchRequest req) {
        if (req.getSportTypeId() != null) {
            preds.add(cb.equal(root.get("sportType").get("sportTypeId"), req.getSportTypeId()));
        }
        if (StringUtils.hasText(req.getAddress())) {
            preds.add(cb.like(cb.lower(root.get("address")), "%" + req.getAddress().toLowerCase() + "%"));
        }
        if (req.getMinPrice() != null) {
            preds.add(cb.greaterThanOrEqualTo(root.get("pricePerHour"), req.getMinPrice()));
        }
        if (req.getMaxPrice() != null) {
            preds.add(cb.lessThanOrEqualTo(root.get("pricePerHour"), req.getMaxPrice()));
        }
    }

    private static void addTimeSlotPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, CriteriaQuery<?> query, StadiumSearchRequest req) {
        if (req.getTargetDate() != null && req.getStartTime() != null && req.getEndTime() != null) {
            LocalDateTime startDateTime = LocalDateTime.of(req.getTargetDate(), req.getStartTime());
            LocalDateTime endDateTime = LocalDateTime.of(req.getTargetDate(), req.getEndTime());

            Subquery<Integer> slotSubquery = query.subquery(Integer.class);
            var slotRoot = slotSubquery.from(TimeSlot.class);
            slotSubquery.select(slotRoot.get("stadium").get("stadiumId"));
            
            Predicate stadiumMatch = cb.equal(slotRoot.get("stadium").get("stadiumId"), root.get("stadiumId"));
            Predicate statusMatch = cb.equal(slotRoot.get("slotStatus"), SlotStatus.AVAILABLE);
            Predicate timeMatch = cb.and(
                    cb.greaterThanOrEqualTo(slotRoot.get("startTime"), startDateTime),
                    cb.lessThanOrEqualTo(slotRoot.get("endTime"), endDateTime)
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

            preds.add(cb.between(root.get("latitude"), minLat, maxLat));
            preds.add(cb.between(root.get("longitude"), minLon, maxLon));
        }
    }

    private static void addAmenitiesPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, CriteriaQuery<?> query, List<Integer> amenityIds) {
        if (amenityIds != null && !amenityIds.isEmpty()) {
            Subquery<Long> amenitySubquery = query.subquery(Long.class);
            var subRoot = amenitySubquery.from(Stadium.class);
            Join<Stadium, Amenity> amenityJoin = subRoot.join("amenities");
            
            amenitySubquery.select(cb.countDistinct(amenityJoin.get("amenityId")));
            
            Predicate idMatch = cb.equal(subRoot.get("stadiumId"), root.get("stadiumId"));
            Predicate amenityIn = amenityJoin.get("amenityId").in(amenityIds);
            
            amenitySubquery.where(cb.and(idMatch, amenityIn));
            
            preds.add(cb.equal(amenitySubquery, (long) amenityIds.size()));
        }
    }
}
