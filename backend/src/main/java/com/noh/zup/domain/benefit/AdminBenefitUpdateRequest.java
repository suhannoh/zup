package com.noh.zup.domain.benefit;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdminBenefitUpdateRequest(
        Long brandId,
        @Size(max = 200) String title,
        @Size(max = 500) String summary,
        String detail,
        BenefitType benefitType,
        OccasionType occasionType,
        BirthdayTimingType birthdayTimingType,
        @Size(max = 500) String conditionSummary,
        Boolean requiredApp,
        Boolean requiredMembership,
        Boolean requiredPurchase,
        @Size(max = 100) String membershipGrade,
        @Size(max = 500) String usagePeriodDescription,
        LocalDate availableFrom,
        LocalDate availableTo,
        String caution,
        VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        Boolean isActive
) {
}
