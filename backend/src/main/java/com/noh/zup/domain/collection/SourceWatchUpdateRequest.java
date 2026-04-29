package com.noh.zup.domain.collection;

import com.noh.zup.domain.source.SourceType;
import jakarta.validation.constraints.Size;

public record SourceWatchUpdateRequest(
        SourceType sourceType,
        @Size(max = 300) String title,
        @Size(max = 1000) String url,
        Boolean isActive,
        Boolean loginRequired,
        RobotsCheckStatus robotsCheckStatus,
        TermsCheckStatus termsCheckStatus,
        CollectionMethod collectionMethod,
        CollectionPermissionStatus collectionPermissionStatus,
        @Size(max = 2000) String policyCheckNote,
        @Size(max = 2000) String manualVerificationNote
) {
}
