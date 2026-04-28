package com.noh.zup.domain.category;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Integer displayOrder
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDisplayOrder()
        );
    }
}
