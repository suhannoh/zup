package com.noh.zup.domain.benefit;

public record BenefitSearchCondition(
        String brandSlug,
        String categorySlug,
        String tagSlug,
        BenefitType benefitType,
        BirthdayTimingType birthdayTimingType,
        Boolean requiredApp,
        Boolean requiredMembership,
        Boolean requiredPurchase
) {
}
