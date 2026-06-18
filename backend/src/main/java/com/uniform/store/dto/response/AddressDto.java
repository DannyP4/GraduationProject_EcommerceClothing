package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.ShippingRegion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto {
    private Long id;
    private String label;
    private String recipient;
    private String phone;
    private String line1;
    private String ward;
    private String district;
    private String city;
    private String country;
    private String postalCode;
    private ShippingRegion region;
    private Integer ghnProvinceId;
    private Integer ghnDistrictId;
    private String ghnWardCode;
    private Boolean isDefault;
}
