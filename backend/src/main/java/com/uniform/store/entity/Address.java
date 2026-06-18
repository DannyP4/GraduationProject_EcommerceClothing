package com.uniform.store.entity;

import com.uniform.store.enums.ShippingRegion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_addresses_user"))
    private User user;

    @Column(name = "label", length = 50)
    private String label;

    @Column(name = "recipient", nullable = false, length = 150)
    private String recipient;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "country", nullable = false, length = 2)
    @Builder.Default
    private String country = "VN";

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 10)
    private ShippingRegion region;

    @Column(name = "ghn_province_id")
    private Integer ghnProvinceId;

    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Column(name = "ghn_ward_code", length = 20)
    private String ghnWardCode;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
}
