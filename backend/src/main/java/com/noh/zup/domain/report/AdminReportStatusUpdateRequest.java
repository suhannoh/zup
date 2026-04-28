package com.noh.zup.domain.report;

import jakarta.validation.constraints.NotNull;

public record AdminReportStatusUpdateRequest(
        @NotNull ReportStatus status,
        String adminMemo
) {
}
