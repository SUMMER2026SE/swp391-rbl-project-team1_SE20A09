package com.sportvenue.repository.specification;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingSpecification {

    public static Specification<Booking> withDynamicFilter(
            String search,
            BookingStatus bookingStatus,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            Integer stadiumId,
            Integer ownerId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Shared Join variable to prevent duplicate left joins
            Join<Booking, Stadium> stadiumJoin = null;

            if (StringUtils.hasText(search)) {
                stadiumJoin = root.join("stadium", JoinType.LEFT);
                predicates.add(buildSearchPredicate(root, cb, stadiumJoin, search));
            }

            if (bookingStatus != null) {
                predicates.add(cb.equal(root.get("bookingStatus"), bookingStatus));
            }

            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reservationDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reservationDate"), endDate));
            }

            if (stadiumId != null) {
                predicates.add(cb.equal(root.get("stadium").get("stadiumId"), stadiumId));
            }

            if (ownerId != null) {
                Join<Booking, Stadium> ownerStadiumJoin =
                        stadiumJoin != null ? stadiumJoin : root.join("stadium", JoinType.LEFT);
                predicates.add(buildOwnerPredicate(cb, ownerStadiumJoin, ownerId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Search theo tên/email/SĐT khách hàng hoặc tên sân (kể cả Facility cha). */
    private static Predicate buildSearchPredicate(Root<Booking> root, CriteriaBuilder cb,
            Join<Booking, Stadium> stadiumJoin, String search) {
        String likePattern = "%" + search.trim().toLowerCase() + "%";

        Join<Booking, User> userJoin = root.join("user", JoinType.LEFT);
        Join<Stadium, Stadium> parentStadiumJoin = stadiumJoin.join("parentStadium", JoinType.LEFT);

        Predicate emailLike = cb.like(cb.lower(userJoin.get("email")), likePattern);
        Predicate firstNameLike = cb.like(cb.lower(userJoin.get("firstName")), likePattern);
        Predicate lastNameLike = cb.like(cb.lower(userJoin.get("lastName")), likePattern);

        // Hỗ trợ tìm kiếm theo họ tên đầy đủ ghép lại
        Expression<String> fullNameExpr = cb.concat(
                cb.concat(cb.lower(userJoin.get("lastName")), " "),
                cb.lower(userJoin.get("firstName"))
        );
        Predicate fullNameLike = cb.like(fullNameExpr, likePattern);
        Predicate phoneLike = cb.like(cb.lower(userJoin.get("phoneNumber")), likePattern);
        Predicate stadiumNameLike = cb.like(cb.lower(stadiumJoin.get("stadiumName")), likePattern);
        Predicate parentStadiumNameLike = cb.like(cb.lower(parentStadiumJoin.get("stadiumName")), likePattern);

        return cb.or(emailLike, firstNameLike, lastNameLike, fullNameLike,
                phoneLike, stadiumNameLike, parentStadiumNameLike);
    }

    /**
     * Lọc theo ownerId — OR-join qua cả 3 nhánh của mô hình phân cấp sân:
     * owner trực tiếp trên Stadium, owner của Complex cha (Court -> Facility -> Complex),
     * và owner của Complex gắn trực tiếp (Facility -> Complex).
     */
    private static Predicate buildOwnerPredicate(CriteriaBuilder cb, Join<Booking, Stadium> stadiumJoin,
            Integer ownerId) {
        Join<Stadium, Owner> directOwner = stadiumJoin.join("owner", JoinType.LEFT);
        Join<Stadium, Stadium> parentStadium = stadiumJoin.join("parentStadium", JoinType.LEFT);
        Join<Stadium, StadiumComplex> parentComplex = parentStadium.join("complex", JoinType.LEFT);
        Join<StadiumComplex, Owner> parentComplexOwner = parentComplex.join("owner", JoinType.LEFT);

        Join<Stadium, StadiumComplex> directComplex = stadiumJoin.join("complex", JoinType.LEFT);
        Join<StadiumComplex, Owner> directComplexOwner = directComplex.join("owner", JoinType.LEFT);

        return cb.or(
                cb.equal(directOwner.get("ownerId"), ownerId),
                cb.equal(parentComplexOwner.get("ownerId"), ownerId),
                cb.equal(directComplexOwner.get("ownerId"), ownerId)
        );
    }
}
