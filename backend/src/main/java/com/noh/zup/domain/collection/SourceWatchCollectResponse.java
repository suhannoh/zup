package com.noh.zup.domain.collection;

public record SourceWatchCollectResponse(
        Long sourceWatchId,
        boolean fetched,
        boolean sameAsPrevious,
        int candidateCount,
        String message
) {
}
