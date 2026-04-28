package com.noh.zup.domain.collection;

import com.noh.zup.domain.source.SourceType;
import java.time.LocalDateTime;

public record SourceWatchResponse(
        Long id,
        Long brandId,
        String brandName,
        SourceType sourceType,
        String title,
        String url,
        Boolean isActive,
        LocalDateTime lastFetchedAt,
        String lastContentHash,
        SourceWatchStatus lastStatus,
        Integer failureCount,
        LocalDateTime nextFetchAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SourceWatchResponse from(SourceWatch sourceWatch) {
        return new SourceWatchResponse(
                sourceWatch.getId(),
                sourceWatch.getBrand().getId(),
                sourceWatch.getBrand().getName(),
                sourceWatch.getSourceType(),
                sourceWatch.getTitle(),
                sourceWatch.getUrl(),
                sourceWatch.getIsActive(),
                sourceWatch.getLastFetchedAt(),
                sourceWatch.getLastContentHash(),
                sourceWatch.getLastStatus(),
                sourceWatch.getFailureCount(),
                sourceWatch.getNextFetchAt(),
                sourceWatch.getCreatedAt(),
                sourceWatch.getUpdatedAt()
        );
    }
}
