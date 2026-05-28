package com.uniform.store.repository;

import com.uniform.store.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    boolean existsByVariantId(Long variantId);

    // Bulk fetch for the orders list page
    // avoids N+1 when summarising multiple orders.
    @Query("""
        SELECT oi FROM OrderItem oi
        WHERE oi.order.id IN :orderIds
        ORDER BY oi.order.id ASC, oi.id ASC
        """)
    List<OrderItem> findByOrderIdInOrderByOrderIdAscIdAsc(@Param("orderIds") Collection<Long> orderIds);
}
