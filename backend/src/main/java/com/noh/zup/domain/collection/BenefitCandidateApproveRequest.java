package com.noh.zup.domain.collection;

import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;

public record BenefitCandidateApproveRequest(
        String title,
        String summary,
        BenefitType benefitType,
        OccasionType occasionType,
        BirthdayTimingType birthdayTimingType,
        String birthdayTimingDescription,
        Boolean requiresApp,
        Boolean requiresSignup,
        Boolean requiresMembership,
        String minimumPurchaseDescription,
        String usageCondition,
        String adminMemo
) {
}
