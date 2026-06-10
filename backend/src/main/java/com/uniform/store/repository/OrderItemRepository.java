package com.uniform.store.repository;

import com.uniform.store.entity.OrderItem;
import com.uniform.store.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
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

    // Total units sold across all products of a brand, over real-sale orders.
    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.variant.product.brand.id = :brandId AND oi.order.status IN :statuses
        """)
    long sumSoldByBrandId(@Param("brandId") Long brandId, @Param("statuses") Collection<OrderStatus> statuses);

    // Trending = best-selling active products, by units sold over sale-bearing orders.
    @Query("""
        SELECT oi.variant.product.id
        FROM OrderItem oi
        WHERE oi.order.status IN :statuses
          AND oi.variant.product.isActive = true
          AND oi.variant.product.deletedAt IS NULL
        GROUP BY oi.variant.product.id
        ORDER BY SUM(oi.quantity) DESC
        """)
    List<Long> findTopSellingProductIds(@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);

    // Frequently bought together: co-occurring products in real-sale orders, excluding self / inactive / out-of-stock
    @Query("""
        SELECT oi2.variant.product.id
        FROM OrderItem oi1, OrderItem oi2
        WHERE oi1.order = oi2.order
          AND oi1.variant.product.id = :productId
          AND oi2.variant.product.id <> :productId
          AND oi1.order.status IN :statuses
          AND oi2.variant.product.isActive = true
          AND oi2.variant.product.deletedAt IS NULL
          AND EXISTS (SELECT 1 FROM ProductVariant v
                      WHERE v.product = oi2.variant.product
                        AND v.isActive = true
                        AND v.stockQuantity > 0)
        GROUP BY oi2.variant.product.id
        ORDER BY COUNT(DISTINCT oi1.order.id) DESC
        """)
    List<Long> findFrequentlyBoughtTogether(@Param("productId") Long productId,
                                            @Param("statuses") Collection<OrderStatus> statuses,
                                            Pageable pageable);
}
