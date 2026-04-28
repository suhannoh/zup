package com.noh.zup.domain.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdminBenefitSourceCreateRequest(
        @NotNull SourceType sourceType,
        @NotBlank @Size(max = 1000) String sourceUrl,
        @Size(max = 300) String sourceTitle,
        LocalDate sourceCheckedAt,
        String memo
) {
}
