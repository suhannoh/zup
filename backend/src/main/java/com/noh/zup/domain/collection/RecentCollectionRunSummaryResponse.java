package com.noh.zup.domain.collection;

import java.time.LocalDateTime;

public record RecentCollectionRunSummaryResponse(
        Long id,
        CollectionTriggerType triggerType,
        CollectionRunStatus status,
        LocalDateTime startedAt,
        Boolean fetched,
        Boolean sameAsPrevious,
        Integer candidateCount,
        String failureReason,
        String errorMessage
) {
    public static RecentCollectionRunSummaryResponse from(CollectionRun run) {
        return new RecentCollectionRunSummaryResponse(
                run.getId(),
                run.getTriggerType(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFetched(),
                run.getSameAsPrevious(),
                run.getCandidateCount(),
                run.getFailureReason(),
                run.getErrorMessage()
        );
    }
}
