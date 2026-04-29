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
        String conditionSummary,
        Boolean requiredApp,
        Boolean requiredMembership,
        Boolean requiredPurchase,
        String membershipGrade,
        String usagePeriodDescription,
        LocalDate availableFrom,
        LocalDate availableTo,
        String caution,
        LocalDate lastVerifiedAt,
        List<BenefitTagResponse> tags,
        List<BenefitSourceResponse> sources,
        List<PublicBenefitDetailItemResponse> detailItems
) {
    public static BenefitListResponse of(
            Benefit benefit,
            List<BenefitTagResponse> tags,
            List<BenefitSourceResponse> sources,
            List<PublicBenefitDetailItemResponse> detailItems
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
                benefit.getConditionSummary(),
                benefit.getRequiredApp(),
                benefit.getRequiredMembership(),
                benefit.getRequiredPurchase(),
                benefit.getMembershipGrade(),
                benefit.getUsagePeriodDescription(),
                benefit.getAvailableFrom(),
                benefit.getAvailableTo(),
                benefit.getCaution(),
                benefit.getLastVerifiedAt(),
                tags,
                sources,
                detailItems
        );
    }
}
