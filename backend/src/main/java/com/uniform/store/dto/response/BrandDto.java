package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String slug;
    private String name;
    private String description;
    private String logoUrl;
    private String websiteUrl;
}
