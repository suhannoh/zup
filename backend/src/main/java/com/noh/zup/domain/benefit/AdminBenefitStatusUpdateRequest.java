package com.noh.zup.domain.benefit;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AdminBenefitStatusUpdateRequest(
        @NotNull VerificationStatus verificationStatus,
        LocalDate lastVerifiedAt,
        String memo
) {
}
