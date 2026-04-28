package com.noh.zup.domain.brand;

import com.noh.zup.domain.benefit.BenefitListResponse;
import java.util.List;

public record BrandDetailResponse(
        Long id,
        String name,
        String slug,
        String categoryName,
        String categorySlug,
        String description,
        String officialUrl,
        String membershipUrl,
        String appUrl,
        String brandColor,
        String logoUrl,
        List<BenefitListResponse> benefits
) {
    public static BrandDetailResponse of(Brand brand, List<BenefitListResponse> benefits) {
        return new BrandDetailResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getCategory().getName(),
                brand.getCategory().getSlug(),
                brand.getDescription(),
                brand.getOfficialUrl(),
                brand.getMembershipUrl(),
                brand.getAppUrl(),
                brand.getBrandColor(),
                brand.getLogoUrl(),
                benefits
        );
    }
}
