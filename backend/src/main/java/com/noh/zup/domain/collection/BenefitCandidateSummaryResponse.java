package com.noh.zup.domain.collection;

import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.OccasionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BenefitCandidateSummaryResponse(
        Long id,
        String title,
        Long brandId,
        String brandName,
        BenefitCandidateStatus status,
        BigDecimal confidence,
        Long sourceWatchId,
        String sourceWatchTitle,
        Long collectionRunId,
        Long snapshotId,
        LocalDateTime createdAt,
        BenefitType benefitType,
        OccasionType applicableTiming,
        Boolean needsManualReview,
        int warningCount,
        int excludedTextCount
) {
    public static BenefitCandidateSummaryResponse from(BenefitCandidate candidate, Long collectionRunId) {
        return new BenefitCandidateSummaryResponse(
                candidate.getId(),
                candidate.getTitle(),
                candidate.getBrand().getId(),
                candidate.getBrand().getName(),
                candidate.getStatus(),
                candidate.getConfidence(),
                candidate.getSourceWatch().getId(),
                candidate.getSourceWatch().getTitle(),
                collectionRunId,
                candidate.getSnapshot().getId(),
                candidate.getCreatedAt(),
                candidate.getBenefitType(),
                candidate.getOccasionType(),
                candidate.getNeedsManualReview(),
                countEntries(candidate.getExtractionWarnings()),
                countEntries(candidate.getExcludedTexts())
        );
    }

    private static int countEntries(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return (int) value.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .count();
    }
}
