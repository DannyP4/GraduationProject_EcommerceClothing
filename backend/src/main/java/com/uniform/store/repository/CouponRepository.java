package com.uniform.store.repository;

import com.uniform.store.entity.Coupon;
import com.uniform.store.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select c from Coupon c where (:status is null or c.status = :status) "
            + "and (:q is null or lower(c.code) like lower(concat('%', :q, '%')))")
    Page<Coupon> searchAdmin(@Param("status") CouponStatus status, @Param("q") String q, Pageable pageable);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 "
            + "where c.id = :id and (c.maxUses is null or c.usedCount < c.maxUses)")
    int incrementUsage(@Param("id") Long id);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount - 1 where c.id = :id and c.usedCount > 0")
    int decrementUsage(@Param("id") Long id);
}
