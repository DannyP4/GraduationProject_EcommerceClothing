package com.uniform.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orderitem_order"))
    private Order order;

    /**
     * Nullable — set to NULL if the variant is later deleted.
     * Always read snapshot columns for display, never follow this FK for order history.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id",
            foreignKey = @ForeignKey(name = "fk_orderitem_variant"))
    private ProductVariant variant;

    // ── Snapshot columns — frozen at purchase time ───────────
    @Column(name = "product_id_snapshot", nullable = false)
    private Long productIdSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "product_slug_snapshot", nullable = false, length = 280)
    private String productSlugSnapshot;

    @Column(name = "sku_snapshot", nullable = false, length = 100)
    private String skuSnapshot;

    @Column(name = "size_snapshot", nullable = false, length = 20)
    private String sizeSnapshot;

    @Column(name = "color_snapshot", nullable = false, length = 60)
    private String colorSnapshot;

    @Column(name = "image_url_snapshot", length = 2048)
    private String imageUrlSnapshot;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private short quantity;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
