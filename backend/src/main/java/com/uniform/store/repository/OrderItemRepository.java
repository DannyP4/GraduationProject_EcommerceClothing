package com.uniform.store.repository;

import com.uniform.store.entity.OrderItem;
import com.uniform.store.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    boolean existsByVariantId(Long variantId);

    // Verified-purchase gate for reviews.
    boolean existsByOrderUserIdAndVariantProductIdAndOrderStatus(Long userId, Long productId, OrderStatus status);

    // Most recent delivered purchase, to attach variant + order to the review.
    Optional<OrderItem> findFirstByOrderUserIdAndVariantProductIdAndOrderStatusOrderByOrderPlacedAtDescIdDesc(
            Long userId, Long productId, OrderStatus status);

    // Bulk fetch for the orders list page
    // avoids N+1 when summarising multiple orders.
    @Query("""
        SELECT oi FROM OrderItem oi
        WHERE oi.order.id IN :orderIds
        ORDER BY oi.order.id ASC, oi.id ASC
        """)
    List<OrderItem> findByOrderIdInOrderByOrderIdAscIdAsc(@Param("orderIds") Collection<Long> orderIds);

    // Units sold per product, counting orders that reached a real sale.
    @Query("""
        SELECT oi.variant.product.id, SUM(oi.quantity)
        FROM OrderItem oi
        WHERE oi.variant.product.id IN :productIds AND oi.order.status IN :statuses
        GROUP BY oi.variant.product.id
        """)
    List<Object[]> aggregateSoldByProductIds(@Param("productIds") Collection<Long> productIds,
                                             @Param("statuses") Collection<OrderStatus> statuses);
}
