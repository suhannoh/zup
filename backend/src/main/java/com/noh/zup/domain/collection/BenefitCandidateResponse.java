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
        String sourceWatchTitle,
        Long snapshotId,
        Long collectionRunId,
        String title,
        String summary,
        BenefitType benefitType,
        OccasionType occasionType,
        BirthdayTimingType birthdayTimingType,
        Boolean requiresApp,
        Boolean requiresSignup,
        Boolean requiresMembership,
        String evidenceText,
        String benefitDetailText,
        String benefitDetailImageSources,
        String usageGuideText,
        String extractionWarnings,
        String contextEvidence,
        String excludedTexts,
        BigDecimal confidence,
        BenefitCandidateStatus status,
        Boolean needsManualReview,
        String reviewMemo,
        Long approvedBenefitId,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BenefitCandidateResponse from(BenefitCandidate candidate, Long collectionRunId) {
        return new BenefitCandidateResponse(
                candidate.getId(),
                candidate.getBrand().getId(),
                candidate.getBrand().getName(),
                candidate.getSourceWatch().getId(),
                candidate.getSourceWatch().getTitle(),
                candidate.getSnapshot().getId(),
                collectionRunId,
                candidate.getTitle(),
                candidate.getSummary(),
                candidate.getBenefitType(),
                candidate.getOccasionType(),
                candidate.getBirthdayTimingType(),
                candidate.getRequiresApp(),
                candidate.getRequiresSignup(),
                candidate.getRequiresMembership(),
                candidate.getEvidenceText(),
                candidate.getBenefitDetailText(),
                candidate.getBenefitDetailImageSources(),
                candidate.getUsageGuideText(),
                candidate.getExtractionWarnings(),
                candidate.getContextEvidence(),
                candidate.getExcludedTexts(),
                candidate.getConfidence(),
                candidate.getStatus(),
                candidate.getNeedsManualReview(),
                candidate.getReviewMemo(),
                candidate.getApprovedBenefit() == null ? null : candidate.getApprovedBenefit().getId(),
                candidate.getApprovedAt(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
