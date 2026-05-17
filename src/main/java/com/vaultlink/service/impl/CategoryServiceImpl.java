package com.vaultlink.service.impl;

import com.vaultlink.dto.request.CategoryRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.CategoryResponse;
import com.vaultlink.entity.Category;
import com.vaultlink.repository.CategoryRepository;
import com.vaultlink.repository.DocumentRepository;
import com.vaultlink.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    // -------------------------------------------------------
    // GET ALL CATEGORIES
    // -------------------------------------------------------

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");

        // Step 1: Fetch all categories from the repository
        List<Category> categories = categoryRepository.findAll();

        // Step 2 & 3: For each category, count its documents and map to response
        List<CategoryResponse> responses = categories.stream()
                .map(category -> {
                    int count = documentRepository.countByCategoryId(category.getId()).intValue();
                    return mapToResponse(category, count);
                })
                .collect(java.util.stream.Collectors.toList());

        log.info("Returning {} categories", responses.size());
        return responses;
    }

    // -------------------------------------------------------
    // GET CATEGORY BY ID
    // -------------------------------------------------------

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category with id: {}", id);

        // Step 1: Find category or throw
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", id);
                    return new RuntimeException("Category not found with id: " + id);
                });

        // Step 2: Map to response with document count
        int count = documentRepository.countByCategoryId(id).intValue();
        log.info("Found category '{}' with {} documents", category.getName(), count);
        return mapToResponse(category, count);
    }

    // -------------------------------------------------------
    // CREATE CATEGORY
    // -------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category with name: '{}'", request.getName());

        // Step 1 & 2: Check for duplicate name
        if (categoryRepository.existsByName(request.getName())) {
            log.warn("Category creation failed — name already exists: '{}'", request.getName());
            throw new RuntimeException("Category already exists: " + request.getName());
        }

        // Step 3: Build and save the new Category entity
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", saved.getId());

        // Step 4: Return CategoryResponse (new category has 0 documents)
        return mapToResponse(saved, 0);
    }

    // -------------------------------------------------------
    // UPDATE CATEGORY
    // -------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with id: {}", id);

        // Step 1: Find category or throw
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", id);
                    return new RuntimeException("Category not found with id: " + id);
                });

        // Step 2: Update fields
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        // Step 3: Save and return updated response
        Category updated = categoryRepository.save(category);
        int count = documentRepository.countByCategoryId(id).intValue();

        log.info("Category updated successfully: id={}, name='{}'", updated.getId(), updated.getName());
        return mapToResponse(updated, count);
    }

    // -------------------------------------------------------
    // DELETE CATEGORY
    // -------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        // Step 1: Find category or throw
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", id);
                    return new RuntimeException("Category not found with id: " + id);
                });

        // Step 2: Delete
        categoryRepository.delete(category);
        log.info("Category deleted successfully: id={}, name='{}'", id, category.getName());

        // Step 3: Return success response
        return ApiResponse.success("Category deleted successfully");
    }

    // -------------------------------------------------------
    // PRIVATE HELPER
    // -------------------------------------------------------

    private CategoryResponse mapToResponse(Category category, int documentCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .documentCount(documentCount)
                .build();
    }
}
