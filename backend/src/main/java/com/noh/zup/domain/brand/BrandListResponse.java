package com.noh.zup.domain.brand;

public record BrandListResponse(
        Long id,
        String name,
        String slug,
        String categoryName,
        String categorySlug,
        String description,
        String brandColor,
        String logoUrl
) {
    public static BrandListResponse from(Brand brand) {
        return new BrandListResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getCategory().getName(),
                brand.getCategory().getSlug(),
                brand.getDescription(),
                brand.getBrandColor(),
                brand.getLogoUrl()
        );
    }
}
