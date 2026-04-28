package com.noh.zup.domain.collection;

import java.util.List;

public record CollectionSummaryResponse(
        long totalSourceWatchCount,
        long activeSourceWatchCount,
        long pendingCandidateCount,
        long recentSuccessRunCount,
        long recentFailedRunCount,
        long recentSkippedRunCount,
        List<CollectionFailedRunSummaryResponse> recentFailedRuns
) {
}
