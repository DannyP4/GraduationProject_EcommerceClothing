package com.uniform.store.entity;

import com.uniform.store.enums.ViewMode;
import com.uniform.store.enums.VtoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vto_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VtoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vto_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vto_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id",
            foreignKey = @ForeignKey(name = "fk_vto_variant"))
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private VtoStatus status = VtoStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "view_mode", nullable = false, length = 10)
    private ViewMode viewMode = ViewMode.FRONT;

    /** 0–100 matching the opacity slider in the VTO UI */
    @Column(name = "opacity_value", nullable = false)
    private short opacityValue = 100;

    @Column(name = "added_to_cart", nullable = false)
    private boolean addedToCart = false;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "vtoSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VtoPhoto> photos = new ArrayList<>();
}
