package com.uniform.store.repository.spec;

import com.uniform.store.entity.Order;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

public final class OrderSpecs {

    private OrderSpecs() {}

    public static Specification<Order> hasStatus(OrderStatus status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> placedAtOrAfter(Instant from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("placedAt"), from);
    }

    public static Specification<Order> placedBefore(Instant to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThan(root.get("placedAt"), to);
    }

    public static Specification<Order> matchesSearch(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String like = "%" + raw.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, q, cb) -> {
            Join<Order, User> user = root.join("user");
            Predicate byOrderNum = cb.like(cb.lower(root.get("orderNumber")), like);
            Predicate byEmail    = cb.like(cb.lower(user.get("email")),       like);
            Predicate byName     = cb.like(cb.lower(user.get("fullName")),    like);
            return cb.or(byOrderNum, byEmail, byName);
        };
    }
}
