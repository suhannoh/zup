package com.noh.zup.domain.benefit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BenefitDetailResponse(
        Long id,
        Long brandId,
        String brandName,
        String brandSlug,
        String categoryName,
        String categorySlug,
        String title,
        String summary,
        String detail,
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
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BenefitTagResponse> tags,
        List<BenefitSourceResponse> sources
) {
    public static BenefitDetailResponse of(
            Benefit benefit,
            List<BenefitTagResponse> tags,
            List<BenefitSourceResponse> sources
    ) {
        return new BenefitDetailResponse(
                benefit.getId(),
                benefit.getBrand().getId(),
                benefit.getBrand().getName(),
                benefit.getBrand().getSlug(),
                benefit.getBrand().getCategory().getName(),
                benefit.getBrand().getCategory().getSlug(),
                benefit.getTitle(),
                benefit.getSummary(),
                benefit.getDetail(),
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
                benefit.getVerificationStatus(),
                benefit.getLastVerifiedAt(),
                benefit.getIsActive(),
                benefit.getCreatedAt(),
                benefit.getUpdatedAt(),
                tags,
                sources
        );
    }
}
