package com.noh.zup.domain.collection;

import com.noh.zup.domain.benefit.VerificationStatus;

public record BenefitCandidateApproveResponse(
        Long candidateId,
        Long benefitId,
        VerificationStatus verificationStatus,
        String message
) {
}
