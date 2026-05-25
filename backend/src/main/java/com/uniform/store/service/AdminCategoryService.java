package com.uniform.store.service;

import com.uniform.store.dto.request.CreateCategoryRequest;
import com.uniform.store.dto.request.UpdateCategoryRequest;
import com.uniform.store.dto.response.AdminCategoryDto;

import java.util.List;

public interface AdminCategoryService {

    List<AdminCategoryDto> listTree();

    AdminCategoryDto get(Long id);

    AdminCategoryDto create(CreateCategoryRequest req);

    AdminCategoryDto update(Long id, UpdateCategoryRequest req);

    void delete(Long id);
}
