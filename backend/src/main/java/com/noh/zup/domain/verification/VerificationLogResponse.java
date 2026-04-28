package com.noh.zup.domain.verification;

import com.noh.zup.domain.benefit.VerificationStatus;
import java.time.LocalDateTime;

public record VerificationLogResponse(
        Long id,
        Long benefitId,
        VerificationStatus beforeStatus,
        VerificationStatus afterStatus,
        String memo,
        String adminEmail,
        LocalDateTime verifiedAt
) {
    public static VerificationLogResponse from(VerificationLog verificationLog) {
        return new VerificationLogResponse(
                verificationLog.getId(),
                verificationLog.getBenefit().getId(),
                verificationLog.getBeforeStatus(),
                verificationLog.getAfterStatus(),
                verificationLog.getMemo(),
                verificationLog.getAdminEmail(),
                verificationLog.getVerifiedAt()
        );
    }
}
