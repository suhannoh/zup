package com.noh.zup.domain.brand;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/brands")
public class AdminBrandController {

    private final AdminBrandService adminBrandService;

    public AdminBrandController(AdminBrandService adminBrandService) {
        this.adminBrandService = adminBrandService;
    }

    @GetMapping
    public ApiResponse<List<AdminBrandResponse>> getBrands(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        return ApiResponse.success(adminBrandService.getBrands(categorySlug, keyword, isActive), "brands fetched");
    }

    @PostMapping
    public ApiResponse<AdminBrandResponse> createBrand(@Valid @RequestBody AdminBrandCreateRequest request) {
        return ApiResponse.success(adminBrandService.createBrand(request), "brand created");
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminBrandResponse> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody AdminBrandUpdateRequest request
    ) {
        return ApiResponse.success(adminBrandService.updateBrand(id, request), "brand updated");
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminBrandResponse> updateActive(
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(adminBrandService.updateActive(id, request.isActive()), "brand active updated");
    }
}
