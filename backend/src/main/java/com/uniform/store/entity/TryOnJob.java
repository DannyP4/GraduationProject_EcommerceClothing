package com.uniform.store.entity;

import com.uniform.store.enums.TryOnStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "try_on_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TryOnJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_try_on_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_try_on_product"))
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TryOnStatus status;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_request_id", length = 128)
    private String providerRequestId;

    @Column(name = "provider_response_url", length = 500)
    private String providerResponseUrl;

    @Column(name = "user_image_url", nullable = false, length = 500)
    private String userImageUrl;

    @Column(name = "garment_image_url", nullable = false, length = 500)
    private String garmentImageUrl;

    @Column(name = "garment_photo_type", nullable = false, length = 16)
    private String garmentPhotoType;

    @Column(name = "category", length = 16)
    private String category;

    @Column(name = "result_image_url", length = 500)
    private String resultImageUrl;

    @Column(name = "result_public_id", length = 255)
    private String resultPublicId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
