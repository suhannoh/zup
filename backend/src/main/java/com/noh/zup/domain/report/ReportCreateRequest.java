package com.noh.zup.domain.report;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        Long brandId,
        Long benefitId,
        @NotNull ReportType reportType,
        @NotBlank @Size(min = 5, max = 2000) String content,
        @Size(max = 1000) String referenceUrl,
        @Email @Size(max = 255) String email
) {
}
