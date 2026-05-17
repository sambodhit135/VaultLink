package com.vaultlink.controller;

import com.vaultlink.dto.request.CategoryRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.CategoryResponse;
import com.vaultlink.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /api/categories
     * Public endpoint — no token required
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("GET /api/categories — fetching all categories");
        List<CategoryResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/categories/{id}
     * Protected endpoint — valid JWT token required
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        log.info("GET /api/categories/{} — fetching category by id", id);
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/categories
     * Protected endpoint — valid JWT token required
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("POST /api/categories — creating category with name: '{}'", request.getName());
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/categories/{id}
     * Protected endpoint — valid JWT token required
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        log.info("PUT /api/categories/{} — updating category", id);
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/categories/{id}
     * Protected endpoint — valid JWT token required
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        log.info("DELETE /api/categories/{} — deleting category", id);
        ApiResponse response = categoryService.deleteCategory(id);
        return ResponseEntity.ok(response);
    }
}
