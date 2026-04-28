package com.noh.zup.domain.collection;

import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import jakarta.validation.Valid;
import java.util.List;

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
        String adminMemo,
        @Valid List<BenefitDetailItemApproveRequest> detailItems
) {
}
