package com.noh.zup.domain.collection;

import java.time.LocalDateTime;

public record CollectionRunResponse(
        Long id,
        Long sourceWatchId,
        String sourceWatchTitle,
        String sourceWatchUrl,
        Long brandId,
        String brandName,
        CollectionTriggerType triggerType,
        CollectionRunStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationMillis,
        Boolean fetched,
        Boolean sameAsPrevious,
        Integer candidateCount,
        String failureReason,
        String errorMessage
) {
    public static CollectionRunResponse from(CollectionRun run) {
        SourceWatch sourceWatch = run.getSourceWatch();
        return new CollectionRunResponse(
                run.getId(),
                sourceWatch.getId(),
                sourceWatch.getTitle(),
                sourceWatch.getUrl(),
                sourceWatch.getBrand().getId(),
                sourceWatch.getBrand().getName(),
                run.getTriggerType(),
                run.getStatus(),
                run.getStartedAt(),
                run.getEndedAt(),
                run.getDurationMillis(),
                run.getFetched(),
                run.getSameAsPrevious(),
                run.getCandidateCount(),
                run.getFailureReason(),
                run.getErrorMessage()
        );
    }
}
