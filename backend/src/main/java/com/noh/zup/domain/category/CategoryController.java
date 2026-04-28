package com.noh.zup.domain.category;

import com.noh.zup.common.response.ApiResponse;
import com.noh.zup.domain.brand.BrandListResponse;
import com.noh.zup.domain.brand.BrandService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final BrandService brandService;

    public CategoryController(CategoryService categoryService, BrandService brandService) {
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(categoryService.getCategories(), "categories fetched");
    }

    @GetMapping("/{slug}/brands")
    public ApiResponse<List<BrandListResponse>> getBrandsByCategory(@PathVariable String slug) {
        return ApiResponse.success(brandService.getBrandsByCategory(slug), "brands fetched");
    }
}
