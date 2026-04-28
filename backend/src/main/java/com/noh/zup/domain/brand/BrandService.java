package com.noh.zup.domain.brand;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.benefit.BenefitListResponse;
import com.noh.zup.domain.benefit.BenefitService;
import com.noh.zup.domain.category.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final BenefitService benefitService;

    public BrandService(
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            BenefitService benefitService
    ) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.benefitService = benefitService;
    }

    @Transactional(readOnly = true)
    public List<BrandListResponse> getBrands(String categorySlug, String keyword) {
        String normalizedCategorySlug = normalize(categorySlug);
        String normalizedKeyword = normalize(keyword);

        List<Brand> brands;
        if (normalizedCategorySlug != null && normalizedKeyword != null) {
            brands = brandRepository.findAllByCategorySlugAndNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(
                    normalizedCategorySlug,
                    normalizedKeyword
            );
        } else if (normalizedCategorySlug != null) {
            brands = brandRepository.findAllByCategorySlugAndIsActiveTrueOrderByNameAsc(normalizedCategorySlug);
        } else if (normalizedKeyword != null) {
            brands = brandRepository.findAllByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(normalizedKeyword);
        } else {
            brands = brandRepository.findAllByIsActiveTrueOrderByNameAsc();
        }

        return brands.stream()
                .map(BrandListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandDetailResponse getBrandDetail(String slug) {
        Brand brand = brandRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Brand not found"));
        List<BenefitListResponse> benefits = benefitService.getPublishedBenefitsByBrandSlug(slug);

        return BrandDetailResponse.of(brand, benefits);
    }

    @Transactional(readOnly = true)
    public List<BrandListResponse> getBrandsByCategory(String categorySlug) {
        categoryRepository.findBySlugAndIsActiveTrue(categorySlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Category not found"));

        return brandRepository.findAllByCategorySlugAndIsActiveTrueOrderByNameAsc(categorySlug).stream()
                .map(BrandListResponse::from)
                .toList();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
