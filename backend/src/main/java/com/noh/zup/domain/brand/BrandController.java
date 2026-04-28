package com.noh.zup.domain.brand;

import com.noh.zup.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ApiResponse<List<BrandListResponse>> getBrands(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(brandService.getBrands(categorySlug, keyword), "brands fetched");
    }

    @GetMapping("/{slug}")
    public ApiResponse<BrandDetailResponse> getBrandDetail(@PathVariable String slug) {
        return ApiResponse.success(brandService.getBrandDetail(slug), "brand fetched");
    }
}
