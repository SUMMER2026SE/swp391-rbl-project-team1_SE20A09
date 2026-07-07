package com.sportvenue.repository.specification;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.StadiumComplex;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

    /**
     * Keyword match trên: tên Court, mô tả, tên Facility cha, địa chỉ Court và địa chỉ Complex.
     * Trước đây chỉ match tên Court + description — người dùng gõ tên Facility ("Sân vận động
     * Cẩm Lệ") ra rỗng vì Court con đặt tên chung chung ("Sân 1"). Dùng LEFT JOIN tường minh
     * để sân không có Facility cha/Complex vẫn được match theo tên chính nó.
     */
    private static void addKeywordPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, String kw) {
        if (StringUtils.hasText(kw)) {
            String likePattern = "%" + kw.toLowerCase() + "%";
            Join<Object, Object> parentJoin = root.join("parentStadium", JoinType.LEFT);
            Join<Object, Object> complexJoin = parentJoin.join("complex", JoinType.LEFT);

            Predicate nameLike = cb.like(cb.lower(root.get("stadiumName")), likePattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), likePattern);
            Predicate parentNameLike = cb.like(cb.lower(parentJoin.get("stadiumName")), likePattern);
            Predicate ownAddressLike = cb.like(cb.lower(root.get("address")), likePattern);
            Predicate complexAddressLike = cb.like(cb.lower(complexJoin.get("address")), likePattern);
            preds.add(cb.or(nameLike, descLike, parentNameLike, ownAddressLike, complexAddressLike));
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
        // Province/district exact-match — chuẩn hoá qua VietnamLocationResolver, thay cho LIKE thô
        // dễ miss khi user gõ dài hơn chuỗi lưu trong DB (vd "Thành phố Hồ Chí Minh" vs "Hồ Chí Minh").
        // Dùng root.get("complex") trực tiếp thay vì root.get("parentStadium").get("complex") như
        // addKeywordPredicate ở trên — cả hai đều hợp lệ vì complex_id được set thẳng trên MỌI row
        // COURT/FACILITY (Stadium.java), không chỉ qua parentStadium.
        if (StringUtils.hasText(req.getProvince())) {
            preds.add(cb.equal(cb.lower(root.get("complex").get("province")), req.getProvince().toLowerCase()));
        }
        if (StringUtils.hasText(req.getDistrict())) {
            preds.add(cb.equal(cb.lower(root.get("complex").get("district")), req.getDistrict().toLowerCase()));
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

    /**
     * Trạng thái booking chiếm chỗ slot — đồng bộ với ACTIVE_STATUSES ở BookingServiceImpl.
     */
    private static final List<BookingStatus> SLOT_BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.PENDING, BookingStatus.CONFIRMED);

    /**
     * Sân phải có ít nhất 1 slot AVAILABLE khớp khoảng giờ yêu cầu; nếu có {@code targetDate},
     * slot đó còn phải CHƯA bị đặt trong ngày đó (đối chiếu bảng booking) — trước đây chỉ check
     * khung giờ mẫu tĩnh nên "tìm sân trống ngày X" vẫn trả sân đã kín lịch ngày X.
     * targetDate không kèm giờ cũng được hỗ trợ: chỉ cần còn bất kỳ slot nào trống ngày đó.
     */
    private static void addTimeSlotPredicate(List<Predicate> preds, CriteriaBuilder cb, Root<Stadium> root, CriteriaQuery<?> query, StadiumSearchRequest req) {
        boolean hasTimeRange = req.getStartTime() != null && req.getEndTime() != null;
        if (!hasTimeRange && req.getTargetDate() == null) {
            return;
        }

        Subquery<Integer> slotSubquery = query.subquery(Integer.class);
        var slotRoot = slotSubquery.from(TimeSlot.class);
        slotSubquery.select(slotRoot.get("stadium").get("stadiumId"));

        List<Predicate> slotPreds = new ArrayList<>();
        slotPreds.add(cb.equal(slotRoot.get("stadium").get("stadiumId"), root.get("stadiumId")));
        slotPreds.add(cb.equal(slotRoot.get("slotStatus"), SlotStatus.AVAILABLE));
        if (hasTimeRange) {
            slotPreds.add(cb.and(
                    cb.lessThan(slotRoot.get("startTime"), req.getEndTime()),
                    cb.greaterThan(slotRoot.get("endTime"), req.getStartTime())
            ));
        }

        if (req.getTargetDate() != null) {
            Subquery<Integer> bookedSubquery = slotSubquery.subquery(Integer.class);
            var bookingRoot = bookedSubquery.from(Booking.class);
            bookedSubquery.select(bookingRoot.get("bookingId"));
            bookedSubquery.where(cb.and(
                    cb.equal(bookingRoot.get("slot").get("slotId"), slotRoot.get("slotId")),
                    cb.equal(bookingRoot.get("reservationDate"), req.getTargetDate()),
                    bookingRoot.get("bookingStatus").in(SLOT_BLOCKING_STATUSES)
            ));
            slotPreds.add(cb.not(cb.exists(bookedSubquery)));
        }

        slotSubquery.where(cb.and(slotPreds.toArray(new Predicate[0])));
        preds.add(cb.exists(slotSubquery));
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
