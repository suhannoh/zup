package com.noh.zup.domain.collection;

import jakarta.validation.constraints.NotNull;

public record BenefitCandidateStatusUpdateRequest(
        @NotNull BenefitCandidateStatus status,
        String reviewMemo
) {
}
