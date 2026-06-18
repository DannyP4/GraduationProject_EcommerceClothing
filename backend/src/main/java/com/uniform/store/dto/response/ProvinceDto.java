package com.uniform.store.dto.response;

import lombok.Getter;

@Getter
public class ProvinceDto {

    private final Integer id;
    private final String name;

    public ProvinceDto(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
