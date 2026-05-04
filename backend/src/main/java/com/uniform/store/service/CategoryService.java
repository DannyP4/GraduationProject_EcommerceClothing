package com.uniform.store.service;

import com.uniform.store.dto.response.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> listCategories(String locale);
}
