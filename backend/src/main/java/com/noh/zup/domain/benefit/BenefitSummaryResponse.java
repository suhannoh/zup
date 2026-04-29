package com.noh.zup.domain.benefit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BenefitSummaryResponse(
        Long id,
        Long brandId,
        String brandName,
        String brandSlug,
        String title,
        String summary,
        BenefitType benefitType,
        OccasionType applicableTiming,
        VerificationStatus verificationStatus,
        Boolean isActive,
        LocalDate lastVerifiedAt,
        long detailItemCount,
        long sourceCount,
        long tagCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BenefitSummaryResponse of(
            Benefit benefit,
            long detailItemCount,
            long sourceCount,
            long tagCount
    ) {
        return new BenefitSummaryResponse(
                benefit.getId(),
                benefit.getBrand().getId(),
                benefit.getBrand().getName(),
                benefit.getBrand().getSlug(),
                benefit.getTitle(),
                benefit.getSummary(),
                benefit.getBenefitType(),
                benefit.getOccasionType(),
                benefit.getVerificationStatus(),
                benefit.getIsActive(),
                benefit.getLastVerifiedAt(),
                detailItemCount,
                sourceCount,
                tagCount,
                benefit.getCreatedAt(),
                benefit.getUpdatedAt()
        );
    }
}
