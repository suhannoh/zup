package com.noh.zup.domain.source;

import com.noh.zup.domain.collection.CollectionMethod;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminBenefitSourceResponse(
        Long id,
        Long benefitId,
        SourceType sourceType,
        String sourceUrl,
        String sourceTitle,
        LocalDate sourceCheckedAt,
        String officialSourceUrl,
        LocalDate lastVerifiedDate,
        CollectionMethod collectionMethod,
        String verificationSummary,
        String sourceNotice,
        String memo,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminBenefitSourceResponse from(BenefitSource source) {
        return new AdminBenefitSourceResponse(
                source.getId(),
                source.getBenefit().getId(),
                source.getSourceType(),
                source.getSourceUrl(),
                source.getSourceTitle(),
                source.getSourceCheckedAt(),
                source.getOfficialSourceUrl(),
                source.getLastVerifiedDate(),
                source.getCollectionMethod(),
                source.getVerificationSummary(),
                source.getSourceNotice(),
                source.getMemo(),
                source.getIsActive(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
