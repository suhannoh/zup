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
        Boolean loginRequired,
        CollectionPermissionStatus collectionPermissionStatus,
        RobotsCheckStatus robotsCheckStatus,
        TermsCheckStatus termsCheckStatus,
        CollectionMethod collectionMethod,
        LocalDateTime lastPolicyCheckedAt,
        LocalDateTime lastManualVerifiedAt,
        String policyCheckNote,
        String manualVerificationNote,
        RecentCollectionRunSummaryResponse recentCollectionRun,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SourceWatchResponse from(
            SourceWatch sourceWatch,
            RecentCollectionRunSummaryResponse recentCollectionRun
    ) {
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
                sourceWatch.getLoginRequired(),
                sourceWatch.getCollectionPermissionStatus(),
                sourceWatch.getRobotsCheckStatus(),
                sourceWatch.getTermsCheckStatus(),
                sourceWatch.getCollectionMethod(),
                sourceWatch.getLastPolicyCheckedAt(),
                sourceWatch.getLastManualVerifiedAt(),
                sourceWatch.getPolicyCheckNote(),
                sourceWatch.getManualVerificationNote(),
                recentCollectionRun,
                sourceWatch.getCreatedAt(),
                sourceWatch.getUpdatedAt()
        );
    }
}
