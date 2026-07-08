package com.sportvenue.repository.specification;

import com.sportvenue.dto.request.StadiumComplexSearchRequest;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class StadiumComplexSpecification {

    public static Specification<StadiumComplex> withDynamicFilter(StadiumComplexSearchRequest req) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // 1. Status Filter - approvedStatus = APPROVED and complexStatus != CLOSED
            predicates.add(cb.equal(root.get("approvedStatus"), ApprovedStatus.APPROVED));
            predicates.add(cb.notEqual(root.get("complexStatus"), ComplexStatus.CLOSED));

            // 2. Keyword Filter (name, description, address)
            if (StringUtils.hasText(req.getKeyword())) {
                String likePattern = "%" + req.getKeyword().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), likePattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
                Predicate addressLike = cb.like(cb.lower(root.get("address")), likePattern);
                predicates.add(cb.or(nameLike, descLike, addressLike));
            }

            // 2b. Province/district exact-match — root đã là StadiumComplex nên truy cập trực tiếp,
            // không cần join. Thay cho keyword LIKE vốn hay miss khi user gõ dài hơn address lưu DB.
            if (StringUtils.hasText(req.getProvince())) {
                predicates.add(cb.equal(cb.lower(root.get("province")), req.getProvince().toLowerCase()));
            }
            if (StringUtils.hasText(req.getDistrict())) {
                predicates.add(cb.equal(cb.lower(root.get("district")), req.getDistrict().toLowerCase()));
            }

            // 3. SportType Filter
            if (req.getSportTypeId() != null) {
                Join<StadiumComplex, SportType> sportTypeJoin = root.join("sportTypes");
                predicates.add(cb.equal(sportTypeJoin.get("sportTypeId"), req.getSportTypeId()));
            }

            // 4. Amenities Filter (Complex must have all specified amenities)
            if (req.getAmenityIds() != null && !req.getAmenityIds().isEmpty()) {
                Subquery<Long> amenitySubquery = query.subquery(Long.class);
                Root<StadiumComplex> subqueryRoot = amenitySubquery.from(StadiumComplex.class);
                Join<StadiumComplex, Amenity> amenityJoin = subqueryRoot.join("amenities");
                amenitySubquery.select(cb.countDistinct(amenityJoin.get("amenityId")));
                amenitySubquery.where(cb.and(
                        cb.equal(subqueryRoot.get("complexId"), root.get("complexId")),
                        amenityJoin.get("amenityId").in(req.getAmenityIds())
                ));
                predicates.add(cb.equal(amenitySubquery, (long) req.getAmenityIds().size()));
            }

            // 5. GPS Filter (bounding box filter)
            if (req.hasLocation()) {
                double latDelta = req.getRadiusInKm() / 111.0;
                double lonDelta = req.getRadiusInKm() / (111.0 * Math.cos(Math.toRadians(req.getUserLat())));

                double minLat = req.getUserLat() - latDelta;
                double maxLat = req.getUserLat() + latDelta;
                double minLon = req.getUserLng() - lonDelta;
                double maxLon = req.getUserLng() + lonDelta;

                predicates.add(cb.between(root.get("latitude"), minLat, maxLat));
                predicates.add(cb.between(root.get("longitude"), minLon, maxLon));
            }

            // 6. Price range and Time Slot availability filters on child COURTs
            addCourtSubqueryPredicate(req, root, query, cb, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addCourtSubqueryPredicate(
            StadiumComplexSearchRequest req,
            Root<StadiumComplex> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<Predicate> predicates) {
        if (req.getMinPrice() == null && req.getMaxPrice() == null &&
            (req.getStartTime() == null || req.getEndTime() == null)) {
            return;
        }

        Subquery<Integer> courtSubquery = query.subquery(Integer.class);
        Root<Stadium> courtRoot = courtSubquery.from(Stadium.class);
        courtSubquery.select(courtRoot.get("stadiumId"));

        List<Predicate> courtPreds = new ArrayList<>();
        courtPreds.add(cb.equal(courtRoot.get("complex").get("complexId"), root.get("complexId")));
        courtPreds.add(cb.equal(courtRoot.get("nodeType"), StadiumNodeType.COURT));
        courtPreds.add(cb.equal(courtRoot.get("stadiumStatus"), StadiumStatus.AVAILABLE));

        if (req.getMinPrice() != null) {
            courtPreds.add(cb.greaterThanOrEqualTo(courtRoot.get("pricePerHour"), req.getMinPrice()));
        }
        if (req.getMaxPrice() != null) {
            courtPreds.add(cb.lessThanOrEqualTo(courtRoot.get("pricePerHour"), req.getMaxPrice()));
        }

        if (req.getStartTime() != null && req.getEndTime() != null) {
            Subquery<Integer> slotSubquery = query.subquery(Integer.class);
            Root<TimeSlot> slotRoot = slotSubquery.from(TimeSlot.class);
            slotSubquery.select(slotRoot.get("stadium").get("stadiumId"));

            List<Predicate> slotPreds = new ArrayList<>();
            slotPreds.add(cb.equal(slotRoot.get("stadium").get("stadiumId"), courtRoot.get("stadiumId")));
            slotPreds.add(cb.equal(slotRoot.get("slotStatus"), SlotStatus.AVAILABLE));
            slotPreds.add(cb.and(
                    cb.lessThan(slotRoot.get("startTime"), req.getEndTime()),
                    cb.greaterThan(slotRoot.get("endTime"), req.getStartTime())
            ));

            slotSubquery.where(cb.and(slotPreds.toArray(new Predicate[0])));
            courtPreds.add(cb.exists(slotSubquery));
        }

        courtSubquery.where(cb.and(courtPreds.toArray(new Predicate[0])));
        predicates.add(cb.exists(courtSubquery));
    }
}
