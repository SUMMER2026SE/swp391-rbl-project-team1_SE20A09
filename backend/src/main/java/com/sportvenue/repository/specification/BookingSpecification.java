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

            // 1. Search keyword
            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";

                Join<Booking, User> userJoin = root.join("user", JoinType.LEFT);
                stadiumJoin = root.join("stadium", JoinType.LEFT);
                Join<Stadium, Stadium> parentStadiumJoin = stadiumJoin.join("parentStadium", JoinType.LEFT);

                // Tên khách hàng (Họ + Tên) ghép chuỗi hoặc tìm kiếm riêng rẽ
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

                predicates.add(cb.or(
                        emailLike,
                        firstNameLike,
                        lastNameLike,
                        fullNameLike,
                        phoneLike,
                        stadiumNameLike,
                        parentStadiumNameLike
                ));
            }

            // 2. Booking Status
            if (bookingStatus != null) {
                predicates.add(cb.equal(root.get("bookingStatus"), bookingStatus));
            }

            // 3. Payment Status
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }

            // 4. Date range (reservationDate)
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reservationDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reservationDate"), endDate));
            }

            // 5. Stadium ID
            if (stadiumId != null) {
                predicates.add(cb.equal(root.get("stadium").get("stadiumId"), stadiumId));
            }

            // 6. Owner ID (with OR-Join for 3-layer model)
            if (ownerId != null) {
                Join<Booking, Stadium> s = stadiumJoin != null ? stadiumJoin : root.join("stadium", JoinType.LEFT);
                Join<Stadium, Owner> o = s.join("owner", JoinType.LEFT);
                Join<Stadium, Stadium> p = s.join("parentStadium", JoinType.LEFT);
                Join<Stadium, StadiumComplex> c = p.join("complex", JoinType.LEFT);
                Join<StadiumComplex, Owner> co = c.join("owner", JoinType.LEFT);
                
                // Direct complex fallback
                Join<Stadium, StadiumComplex> dc = s.join("complex", JoinType.LEFT);
                Join<StadiumComplex, Owner> dco = dc.join("owner", JoinType.LEFT);

                predicates.add(cb.or(
                    cb.equal(o.get("ownerId"), ownerId),
                    cb.equal(co.get("ownerId"), ownerId),
                    cb.equal(dco.get("ownerId"), ownerId)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
