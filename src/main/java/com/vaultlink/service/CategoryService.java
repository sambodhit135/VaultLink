package com.vaultlink.service;

import com.vaultlink.dto.request.CategoryRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    ApiResponse deleteCategory(Long id);
}
