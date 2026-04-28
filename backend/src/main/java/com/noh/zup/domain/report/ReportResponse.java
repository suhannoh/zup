package com.noh.zup.domain.report;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long brandId,
        String brandName,
        Long benefitId,
        String benefitTitle,
        ReportType reportType,
        String content,
        String referenceUrl,
        String email,
        String adminMemo,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static ReportResponse from(UserReport report) {
        return new ReportResponse(
                report.getId(),
                report.getBrand() == null ? null : report.getBrand().getId(),
                report.getBrand() == null ? null : report.getBrand().getName(),
                report.getBenefit() == null ? null : report.getBenefit().getId(),
                report.getBenefit() == null ? null : report.getBenefit().getTitle(),
                report.getReportType(),
                report.getContent(),
                report.getReferenceUrl(),
                report.getEmail(),
                report.getAdminMemo(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}
