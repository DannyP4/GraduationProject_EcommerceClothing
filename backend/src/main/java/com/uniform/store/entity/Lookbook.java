package com.uniform.store.entity;

import com.uniform.store.enums.LookbookGridSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lookbooks")
@SQLDelete(sql = "UPDATE lookbooks SET deleted_at = NOW(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lookbook extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "tag", length = 80)
    private String tag;

    @Column(name = "slug", nullable = false, unique = true, length = 230)
    private String slug;

    @Column(name = "cover_url", nullable = false, length = 2048)
    private String coverUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "grid_size", nullable = false, length = 10)
    private LookbookGridSize gridSize = LookbookGridSize.HALF;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lookbook_products",
            joinColumns = @JoinColumn(name = "lookbook_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"),
            foreignKey = @ForeignKey(name = "fk_lbp_lookbook"),
            inverseForeignKey = @ForeignKey(name = "fk_lbp_product")
    )
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
