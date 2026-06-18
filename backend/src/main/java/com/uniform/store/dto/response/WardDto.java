package com.uniform.store.dto.response;

import lombok.Getter;

@Getter
public class WardDto {

    private final String code;
    private final Integer districtId;
    private final String name;

    public WardDto(String code, Integer districtId, String name) {
        this.code = code;
        this.districtId = districtId;
        this.name = name;
    }
}
