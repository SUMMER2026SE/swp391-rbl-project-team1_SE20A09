package com.sportvenue.repository.specification;

import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class StadiumSpecification {

    public static Specification<Stadium> withDynamicFilter(
            String keyword,
            Integer sportTypeId,
            String address,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate targetDate,
            LocalTime startTime,
            LocalTime endTime,
            Double userLat,
            Double userLng,
            Double radiusInKm,
            List<Integer> amenityIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Only AVAILABLE stadiums
            predicates.add(cb.equal(root.get("stadiumStatus"), StadiumStatus.AVAILABLE));

            // 2. Keyword (Name or Description)
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("stadiumName")), likePattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            // 3. Sport Type
            if (sportTypeId != null) {
                predicates.add(cb.equal(root.get("sportType").get("sportTypeId"), sportTypeId));
            }

            // 4. Address
            if (StringUtils.hasText(address)) {
                predicates.add(cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase() + "%"));
            }

            // 5. Price Range
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerHour"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerHour"), maxPrice));
            }

            // 6. Time Slot Availability (targetDate, startTime, endTime)
            if (targetDate != null && startTime != null && endTime != null) {
                LocalDateTime startDateTime = LocalDateTime.of(targetDate, startTime);
                LocalDateTime endDateTime = LocalDateTime.of(targetDate, endTime);

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
                predicates.add(cb.exists(slotSubquery));
            }

            // 7. Location Bounding Box (GPS Proximity)
            if (userLat != null && userLng != null && radiusInKm != null) {
                // 1 degree of latitude ~= 111 km
                double latDelta = radiusInKm / 111.0;
                // 1 degree of longitude ~= 111 * cos(latitude) km
                double lonDelta = radiusInKm / (111.0 * Math.cos(Math.toRadians(userLat)));

                double minLat = userLat - latDelta;
                double maxLat = userLat + latDelta;
                double minLon = userLng - lonDelta;
                double maxLon = userLng + lonDelta;

                predicates.add(cb.between(root.get("latitude"), minLat, maxLat));
                predicates.add(cb.between(root.get("longitude"), minLon, maxLon));
            }

            // 8. Amenities (AND logic using subquery count)
            if (amenityIds != null && !amenityIds.isEmpty()) {
                Subquery<Long> amenitySubquery = query.subquery(Long.class);
                var subRoot = amenitySubquery.from(Stadium.class);
                Join<Stadium, Amenity> amenityJoin = subRoot.join("amenities");
                
                amenitySubquery.select(cb.countDistinct(amenityJoin.get("amenityId")));
                
                Predicate idMatch = cb.equal(subRoot.get("stadiumId"), root.get("stadiumId"));
                Predicate amenityIn = amenityJoin.get("amenityId").in(amenityIds);
                
                amenitySubquery.where(cb.and(idMatch, amenityIn));
                
                predicates.add(cb.equal(amenitySubquery, (long) amenityIds.size()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
