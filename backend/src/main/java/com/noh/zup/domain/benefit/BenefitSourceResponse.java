package com.noh.zup.domain.benefit;

import com.noh.zup.domain.source.BenefitSource;
import com.noh.zup.domain.source.SourceType;
import java.time.LocalDate;

public record BenefitSourceResponse(
        SourceType sourceType,
        String sourceUrl,
        String sourceTitle,
        LocalDate sourceCheckedAt
) {
    public static BenefitSourceResponse from(BenefitSource source) {
        return new BenefitSourceResponse(
                source.getSourceType(),
                source.getSourceUrl(),
                source.getSourceTitle(),
                source.getSourceCheckedAt()
        );
    }
}
