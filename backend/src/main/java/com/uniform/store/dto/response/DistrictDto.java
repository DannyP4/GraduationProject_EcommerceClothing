package com.uniform.store.dto.response;

import lombok.Getter;

@Getter
public class DistrictDto {

    private final Integer id;
    private final Integer provinceId;
    private final String name;

    public DistrictDto(Integer id, Integer provinceId, String name) {
        this.id = id;
        this.provinceId = provinceId;
        this.name = name;
    }
}
