package com.noh.zup.domain.category;

import com.noh.zup.common.request.ActiveUpdateRequest;
import com.noh.zup.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    @GetMapping
    public ApiResponse<List<AdminCategoryResponse>> getCategories() {
        return ApiResponse.success(adminCategoryService.getCategories(), "categories fetched");
    }

    @PostMapping
    public ApiResponse<AdminCategoryResponse> createCategory(@Valid @RequestBody AdminCategoryCreateRequest request) {
        return ApiResponse.success(adminCategoryService.createCategory(request), "category created");
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody AdminCategoryUpdateRequest request
    ) {
        return ApiResponse.success(adminCategoryService.updateCategory(id, request), "category updated");
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminCategoryResponse> updateActive(
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(adminCategoryService.updateActive(id, request.isActive()), "category active updated");
    }
}
