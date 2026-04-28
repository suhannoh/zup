package com.noh.zup.domain.collection;

import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BenefitCandidateResponse(
        Long id,
        Long brandId,
        String brandName,
        Long sourceWatchId,
        Long snapshotId,
        String title,
        String summary,
        BenefitType benefitType,
        OccasionType occasionType,
        BirthdayTimingType birthdayTimingType,
        Boolean requiresApp,
        Boolean requiresSignup,
        Boolean requiresMembership,
        String evidenceText,
        BigDecimal confidence,
        BenefitCandidateStatus status,
        String reviewMemo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BenefitCandidateResponse from(BenefitCandidate candidate) {
        return new BenefitCandidateResponse(
                candidate.getId(),
                candidate.getBrand().getId(),
                candidate.getBrand().getName(),
                candidate.getSourceWatch().getId(),
                candidate.getSnapshot().getId(),
                candidate.getTitle(),
                candidate.getSummary(),
                candidate.getBenefitType(),
                candidate.getOccasionType(),
                candidate.getBirthdayTimingType(),
                candidate.getRequiresApp(),
                candidate.getRequiresSignup(),
                candidate.getRequiresMembership(),
                candidate.getEvidenceText(),
                candidate.getConfidence(),
                candidate.getStatus(),
                candidate.getReviewMemo(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
