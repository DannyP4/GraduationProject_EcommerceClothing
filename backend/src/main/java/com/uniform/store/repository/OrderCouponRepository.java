package com.uniform.store.repository;

import com.uniform.store.entity.OrderCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderCouponRepository extends JpaRepository<OrderCoupon, Long> {

    Optional<OrderCoupon> findByOrderId(Long orderId);

    boolean existsByCouponId(Long couponId);

    @Query("select count(oc) from OrderCoupon oc where oc.coupon.id = :couponId and oc.order.user.id = :userId")
    long countByCouponAndUser(@Param("couponId") Long couponId, @Param("userId") Long userId);

    void deleteByOrderId(Long orderId);
}
