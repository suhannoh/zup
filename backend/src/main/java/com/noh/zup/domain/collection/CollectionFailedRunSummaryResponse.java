package com.noh.zup.domain.collection;

import java.time.LocalDateTime;

public record CollectionFailedRunSummaryResponse(
        Long runId,
        Long sourceWatchId,
        String sourceWatchTitle,
        String brandName,
        String failureReason,
        String errorMessage,
        LocalDateTime startedAt
) {
    public static CollectionFailedRunSummaryResponse from(CollectionRun run) {
        SourceWatch sourceWatch = run.getSourceWatch();
        return new CollectionFailedRunSummaryResponse(
                run.getId(),
                sourceWatch.getId(),
                sourceWatch.getTitle(),
                sourceWatch.getBrand().getName(),
                run.getFailureReason(),
                run.getErrorMessage(),
                run.getStartedAt()
        );
    }
}
