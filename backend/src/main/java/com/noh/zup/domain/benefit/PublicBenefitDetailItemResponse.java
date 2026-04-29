package com.noh.zup.domain.benefit;

public record PublicBenefitDetailItemResponse(
        Long id,
        String brandName,
        String title,
        String description,
        String conditionText,
        Integer displayOrder,
        Boolean isActive
) {
    public static PublicBenefitDetailItemResponse from(BenefitDetailItem item) {
        return new PublicBenefitDetailItemResponse(
                item.getId(),
                item.getBrandName(),
                item.getTitle(),
                item.getDescription(),
                item.getConditionText(),
                item.getDisplayOrder(),
                item.getIsActive()
        );
    }
}
