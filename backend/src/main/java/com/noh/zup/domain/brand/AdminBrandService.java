package com.noh.zup.domain.brand;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.category.Category;
import com.noh.zup.domain.category.CategoryRepository;
import java.util.Locale;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBrandService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public AdminBrandService(BrandRepository brandRepository, CategoryRepository categoryRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBrandResponse> getBrands(String categorySlug, String keyword, Boolean isActive) {
        String normalizedCategorySlug = normalize(categorySlug);
        String normalizedKeyword = normalize(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);

        return brandRepository.findAllByOrderByNameAsc().stream()
                .filter(brand -> normalizedCategorySlug == null
                        || brand.getCategory().getSlug().equals(normalizedCategorySlug))
                .filter(brand -> lowerKeyword == null
                        || brand.getName().toLowerCase(Locale.ROOT).contains(lowerKeyword))
                .filter(brand -> isActive == null || brand.getIsActive().equals(isActive))
                .map(AdminBrandResponse::from)
                .toList();
    }

    @Transactional
    public AdminBrandResponse createBrand(AdminBrandCreateRequest request) {
        if (brandRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Brand slug already exists");
        }

        Category category = getCategory(request.categoryId());
        Brand brand = new Brand(category, request.name(), request.slug());
        brand.update(
                null,
                null,
                null,
                request.description(),
                request.officialUrl(),
                request.membershipUrl(),
                request.appUrl(),
                request.brandColor(),
                request.logoUrl(),
                request.isActive()
        );
        return AdminBrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public AdminBrandResponse updateBrand(Long id, AdminBrandUpdateRequest request) {
        Brand brand = getBrand(id);
        if (request.slug() != null && brandRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Brand slug already exists");
        }

        Category category = request.categoryId() == null ? null : getCategory(request.categoryId());
        brand.update(
                category,
                request.name(),
                request.slug(),
                request.description(),
                request.officialUrl(),
                request.membershipUrl(),
                request.appUrl(),
                request.brandColor(),
                request.logoUrl(),
                request.isActive()
        );
        return AdminBrandResponse.from(brand);
    }

    @Transactional
    public AdminBrandResponse updateActive(Long id, Boolean isActive) {
        Brand brand = getBrand(id);
        brand.changeActive(isActive);
        return AdminBrandResponse.from(brand);
    }

    private Brand getBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Brand not found"));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Category not found"));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
