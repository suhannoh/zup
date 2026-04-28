package com.noh.zup.domain.brand;

import java.time.LocalDateTime;

public record AdminBrandResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String name,
        String slug,
        String description,
        String officialUrl,
        String membershipUrl,
        String appUrl,
        String brandColor,
        String logoUrl,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminBrandResponse from(Brand brand) {
        return new AdminBrandResponse(
                brand.getId(),
                brand.getCategory().getId(),
                brand.getCategory().getName(),
                brand.getCategory().getSlug(),
                brand.getName(),
                brand.getSlug(),
                brand.getDescription(),
                brand.getOfficialUrl(),
                brand.getMembershipUrl(),
                brand.getAppUrl(),
                brand.getBrandColor(),
                brand.getLogoUrl(),
                brand.getIsActive(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}
