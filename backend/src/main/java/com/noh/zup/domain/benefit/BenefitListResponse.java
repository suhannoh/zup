package com.noh.zup.domain.benefit;

import java.time.LocalDate;
import java.util.List;

public record BenefitListResponse(
        Long id,
        Long brandId,
        String brandName,
        String brandSlug,
        String categoryName,
        String categorySlug,
        String title,
        String summary,
        BenefitType benefitType,
        OccasionType occasionType,
        BirthdayTimingType birthdayTimingType,
        Boolean requiredApp,
        Boolean requiredMembership,
        Boolean requiredPurchase,
        String usagePeriodDescription,
        LocalDate lastVerifiedAt,
        List<BenefitTagResponse> tags,
        List<BenefitSourceResponse> sources
) {
    public static BenefitListResponse of(
            Benefit benefit,
            List<BenefitTagResponse> tags,
            List<BenefitSourceResponse> sources
    ) {
        return new BenefitListResponse(
                benefit.getId(),
                benefit.getBrand().getId(),
                benefit.getBrand().getName(),
                benefit.getBrand().getSlug(),
                benefit.getBrand().getCategory().getName(),
                benefit.getBrand().getCategory().getSlug(),
                benefit.getTitle(),
                benefit.getSummary(),
                benefit.getBenefitType(),
                benefit.getOccasionType(),
                benefit.getBirthdayTimingType(),
                benefit.getRequiredApp(),
                benefit.getRequiredMembership(),
                benefit.getRequiredPurchase(),
                benefit.getUsagePeriodDescription(),
                benefit.getLastVerifiedAt(),
                tags,
                sources
        );
    }
}
