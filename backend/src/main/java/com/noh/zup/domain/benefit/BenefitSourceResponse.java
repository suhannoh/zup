package com.noh.zup.domain.benefit;

import com.noh.zup.domain.source.BenefitSource;
import com.noh.zup.domain.source.SourceType;
import com.noh.zup.domain.collection.CollectionMethod;
import java.time.LocalDate;

public record BenefitSourceResponse(
        SourceType sourceType,
        String sourceUrl,
        String sourceTitle,
        LocalDate sourceCheckedAt,
        String officialSourceUrl,
        LocalDate lastVerifiedDate,
        CollectionMethod collectionMethod,
        String verificationSummary,
        String sourceNotice
) {
    public static BenefitSourceResponse from(BenefitSource source) {
        return new BenefitSourceResponse(
                source.getSourceType(),
                source.getSourceUrl(),
                source.getSourceTitle(),
                source.getSourceCheckedAt(),
                source.getOfficialSourceUrl(),
                source.getLastVerifiedDate(),
                source.getCollectionMethod(),
                source.getVerificationSummary(),
                source.getSourceNotice()
        );
    }
}
