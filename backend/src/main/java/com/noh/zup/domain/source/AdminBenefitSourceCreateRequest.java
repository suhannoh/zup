package com.noh.zup.domain.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.noh.zup.domain.collection.CollectionMethod;
import java.time.LocalDate;

public record AdminBenefitSourceCreateRequest(
        @NotNull SourceType sourceType,
        @NotBlank @Size(max = 1000) String sourceUrl,
        @Size(max = 300) String sourceTitle,
        LocalDate sourceCheckedAt,
        @Size(max = 1000) String officialSourceUrl,
        LocalDate lastVerifiedDate,
        CollectionMethod collectionMethod,
        String verificationSummary,
        String sourceNotice,
        String memo
) {
}
