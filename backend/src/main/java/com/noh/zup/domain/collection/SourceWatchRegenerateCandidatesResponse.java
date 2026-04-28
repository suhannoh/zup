package com.noh.zup.domain.collection;

public record SourceWatchRegenerateCandidatesResponse(
        Long sourceWatchId,
        Long snapshotId,
        int createdCandidateCount,
        int skippedDuplicateCount,
        String message
) {
}
