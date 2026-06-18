package com.uniform.store.repository;

import com.uniform.store.entity.Order;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Page<Order> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    List<Order> findTop50ByUserIdOrderByPlacedAtDesc(Long userId);

    long countByUserId(Long userId);

    // Lookup by orderNumber + userId.
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    long countByStatusIn(Collection<OrderStatus> statuses);

    List<Order> findByStatusInOrderByPlacedAtDescIdDesc(Collection<OrderStatus> statuses, Pageable pageable);

    List<Order> findByStatusAndGhnOrderCodeIsNotNull(OrderStatus status);

    // Excludes COD, only unpaid online-gateway orders go stale.
    @Query("SELECT o.id FROM Order o WHERE o.status = :pending AND o.placedAt < :cutoff "
            + "AND EXISTS (SELECT 1 FROM Payment p WHERE p.order = o AND p.provider <> :cod) "
            + "AND NOT EXISTS (SELECT 1 FROM Payment p2 WHERE p2.order = o AND p2.status = :captured)")
    List<Long> findStalePendingPayableOrderIds(@Param("pending") OrderStatus pending,
                                               @Param("cutoff") Instant cutoff,
                                               @Param("cod") PaymentProvider cod,
                                               @Param("captured") PaymentStatus captured);
}
