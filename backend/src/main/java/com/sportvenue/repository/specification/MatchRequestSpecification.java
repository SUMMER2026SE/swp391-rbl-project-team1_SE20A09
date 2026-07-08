package com.sportvenue.repository.specification;

import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.enums.MatchStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Dùng cho AI chat tool findMatchRequests — lọc kèo ghép đang mở theo khu vực/môn thể thao. */
public class MatchRequestSpecification {

    public static Specification<MatchRequest> withDynamicFilter(
            List<MatchStatus> statuses,
            LocalDate nowDate,
            LocalTime nowTime,
            String location,
            Integer sportTypeId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean isCountQuery = Long.class.equals(query.getResultType());

            Join<MatchRequest, Stadium> stadiumJoin;
            Join<MatchRequest, StadiumComplex> complexJoin;
            if (isCountQuery) {
                stadiumJoin = root.join("stadium", JoinType.LEFT);
                complexJoin = root.join("complex", JoinType.LEFT);
            } else {
                // Fetch-join thay vì EntityGraph tĩnh — tránh N+1 khi map sang MatchResponse.
                // Không dùng query.distinct(true): mọi association đều @ManyToOne (không fan-out),
                // và DISTINCT bắt buộc ORDER BY phải nằm trong SELECT list — xung đột với statusOrder bên dưới.
                root.fetch("user", JoinType.LEFT);
                stadiumJoin = (Join<MatchRequest, Stadium>) root.<MatchRequest, Stadium>fetch("stadium", JoinType.LEFT);
                complexJoin = (Join<MatchRequest, StadiumComplex>) root.<MatchRequest, StadiumComplex>fetch("complex", JoinType.LEFT);
                root.fetch("sportType", JoinType.LEFT);
            }

            predicates.add(root.get("matchStatus").in(statuses));
            predicates.add(cb.or(
                    cb.greaterThan(root.get("playDate"), nowDate),
                    cb.and(cb.equal(root.get("playDate"), nowDate), cb.greaterThan(root.get("startTime"), nowTime))
            ));

            // `location` được chuẩn hoá (province/district canonical) trước khi truyền vào đây
            // (xem MatchRequestHandler) khi khớp được 1 trong 2 thành phố hỗ trợ — nên thử
            // exact-match trước, LIKE address chỉ là fallback cho input lạ ngoài 2 thành phố
            // hoặc tên đường/địa danh cụ thể.
            if (StringUtils.hasText(location)) {
                String exactMatch = location.toLowerCase();
                String likePattern = "%" + exactMatch + "%";
                Predicate provinceMatch = cb.or(
                        cb.equal(cb.lower(complexJoin.get("province")), exactMatch),
                        cb.equal(cb.lower(stadiumJoin.get("complex").get("province")), exactMatch)
                );
                Predicate districtMatch = cb.or(
                        cb.equal(cb.lower(complexJoin.get("district")), exactMatch),
                        cb.equal(cb.lower(stadiumJoin.get("complex").get("district")), exactMatch)
                );
                Predicate addressFallback = cb.or(
                        cb.like(cb.lower(stadiumJoin.get("address")), likePattern),
                        cb.like(cb.lower(complexJoin.get("address")), likePattern)
                );
                predicates.add(cb.or(provinceMatch, districtMatch, addressFallback));
            }

            if (sportTypeId != null) {
                predicates.add(cb.equal(root.get("sportType").get("sportTypeId"), sportTypeId));
            }

            if (!isCountQuery) {
                Expression<Integer> statusOrder = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("matchStatus"), MatchStatus.OPEN), 0)
                        .otherwise(1);
                query.orderBy(
                        cb.asc(statusOrder),
                        cb.asc(root.get("playDate")),
                        cb.asc(root.get("startTime"))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
