package com.noh.zup.domain.collection.candidate;

public record BenefitCandidateDetectionResult(
        int createdCandidateCount,
        int skippedDuplicateCount
) {
}
