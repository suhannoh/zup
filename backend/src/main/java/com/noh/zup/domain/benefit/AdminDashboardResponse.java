package com.noh.zup.domain.benefit;

public record AdminDashboardResponse(
        long brandCount,
        long publishedBenefitCount,
        long draftBenefitCount,
        long needsCheckBenefitCount,
        long expiredBenefitCount,
        long staleBenefitCount,
        long categoryCount,
        long tagCount,
        long receivedReportCount,
        long reviewingReportCount,
        long resolvedReportCount
) {
}
