package com.sportvenue.specification;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class StadiumSpecification {

    public static Specification<Stadium> filterStadiums(StadiumSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Keyword search (name or description)
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("stadiumName")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            // 2. Address/Area search
            if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("address")), "%" + request.getAddress().trim().toLowerCase() + "%"));
            }

            // 3. Sport Type
            if (request.getSportType() != null && !request.getSportType().trim().isEmpty()) {
                predicates.add(cb.equal(root.join("sportType").get("sportName"), request.getSportType()));
            }

            // 4. Price range
            if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerHour"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerHour"), request.getMaxPrice()));
            }

            // 5. Amenities (Subquery AND logic)
            if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
                for (String amenityName : request.getAmenities()) {
                    Subquery<Integer> subquery = query.subquery(Integer.class);
                    Root<Stadium> subRoot = subquery.from(Stadium.class);
                    Join<Stadium, Amenity> amenityJoin = subRoot.join("amenities");
                    subquery.select(subRoot.get("stadiumId"))
                            .where(cb.equal(amenityJoin.get("name"), amenityName));
                    predicates.add(root.get("stadiumId").in(subquery));
                }
            }

            // 6. Distance (Haversine formula approximations using PostGIS or math functions)
            // Since we can't easily write raw SQL Haversine in standard JPA CriteriaBuilder without custom functions,
            // we will approximate a bounding box if latitude and longitude are provided.
            if (request.getLatitude() != null && request.getLongitude() != null && request.getRadius() != null) {
                // 1 degree latitude = ~111km
                double radiusInDegrees = request.getRadius() / 111.0;
                predicates.add(cb.between(root.get("latitude"), request.getLatitude() - radiusInDegrees, request.getLatitude() + radiusInDegrees));
                predicates.add(cb.between(root.get("longitude"), request.getLongitude() - radiusInDegrees, request.getLongitude() + radiusInDegrees));
            }

            // 7. TimeSlot checking
            if (request.getDate() != null) {
                Join<Stadium, TimeSlot> timeSlotJoin = root.join("timeSlots", JoinType.INNER);
                // Make sure to fetch only slots on that date
                predicates.add(cb.between(timeSlotJoin.get("startTime"), request.getDate().atStartOfDay(), request.getDate().plusDays(1).atStartOfDay()));
                predicates.add(cb.equal(timeSlotJoin.get("slotStatus"), "Available"));

                if (request.getStartTime() != null) {
                    predicates.add(cb.lessThanOrEqualTo(timeSlotJoin.get("startTime"), request.getDate().atTime(request.getStartTime())));
                }
                if (request.getEndTime() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(timeSlotJoin.get("endTime"), request.getDate().atTime(request.getEndTime())));
                }
                
                // Use distinct to prevent duplicate stadium entries if multiple slots match
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
