package com.uniform.store.dto.request;

import com.uniform.store.enums.Gender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProductFilterRequest {

    private String search;
    private Long brandId;
    private Long categoryId;
    private Gender gender;
    private Boolean isActive;
    private String deleted = "none";
}
