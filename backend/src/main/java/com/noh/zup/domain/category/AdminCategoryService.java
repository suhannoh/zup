package com.noh.zup.domain.category;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    public AdminCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    @Transactional
    public AdminCategoryResponse createCategory(AdminCategoryCreateRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Category slug already exists");
        }

        Category category = new Category(request.name(), request.slug(), request.displayOrder());
        category.update(null, null, null, request.isActive());
        return AdminCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public AdminCategoryResponse updateCategory(Long id, AdminCategoryUpdateRequest request) {
        Category category = getCategory(id);
        if (request.slug() != null && categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Category slug already exists");
        }

        category.update(request.name(), request.slug(), request.displayOrder(), request.isActive());
        return AdminCategoryResponse.from(category);
    }

    @Transactional
    public AdminCategoryResponse updateActive(Long id, Boolean isActive) {
        Category category = getCategory(id);
        category.changeActive(isActive);
        return AdminCategoryResponse.from(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Category not found"));
    }
}
