package com.noh.zup.domain.collection;

import com.noh.zup.domain.source.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SourceWatchCreateRequest(
        @NotNull Long brandId,
        @NotNull SourceType sourceType,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 1000) String url,
        Boolean isActive,
        Boolean loginRequired,
        RobotsCheckStatus robotsCheckStatus,
        TermsCheckStatus termsCheckStatus,
        CollectionMethod collectionMethod,
        @Size(max = 2000) String policyCheckNote,
        @Size(max = 2000) String manualVerificationNote
) {
}
