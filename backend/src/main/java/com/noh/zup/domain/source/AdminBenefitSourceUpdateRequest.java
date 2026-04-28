package com.noh.zup.domain.source;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdminBenefitSourceUpdateRequest(
        SourceType sourceType,
        @Size(max = 1000) String sourceUrl,
        @Size(max = 300) String sourceTitle,
        LocalDate sourceCheckedAt,
        String memo
) {
}
