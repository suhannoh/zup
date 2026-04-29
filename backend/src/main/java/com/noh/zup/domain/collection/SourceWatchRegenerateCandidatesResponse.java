package com.noh.zup.domain.collection;

public record SourceWatchRegenerateCandidatesResponse(
        Long sourceWatchId,
        Long collectionRunId,
        Long snapshotId,
        int createdCandidateCount,
        int skippedDuplicateCount,
        String failureReason,
        String message
) {
}
