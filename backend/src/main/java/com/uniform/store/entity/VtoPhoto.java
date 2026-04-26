package com.uniform.store.entity;

import com.uniform.store.enums.ViewMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "vto_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VtoPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vto_session_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vtophoto_session"))
    private VtoSession vtoSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vtophoto_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vtophoto_product"))
    private Product product;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "s3_url", nullable = false, length = 2048)
    private String s3Url;

    @Enumerated(EnumType.STRING)
    @Column(name = "view_mode", nullable = false, length = 10)
    private ViewMode viewMode = ViewMode.FRONT;

    @Column(name = "is_shared", nullable = false)
    private boolean isShared = false;

    /** UUID token for /try-on/share/{token} public link */
    @Column(name = "share_token", length = 64)
    private String shareToken;

    @CreationTimestamp
    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;

    /** When set, triggers an async S3 cleanup job */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
