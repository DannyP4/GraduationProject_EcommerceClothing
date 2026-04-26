package com.uniform.store.entity;

import com.uniform.store.enums.ProductSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_variant_sku", columnNames = "sku"),
                @UniqueConstraint(name = "uq_variant_product_size_color", columnNames = {"product_id", "size", "color"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_variant_product"))
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private ProductSize size;

    @Column(name = "color", nullable = false, length = 60)
    private String color;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private short lowStockThreshold = 5;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Optimistic locking — prevents double-selling under concurrent checkout.
     * JPA will include this in UPDATE WHERE clauses automatically.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
