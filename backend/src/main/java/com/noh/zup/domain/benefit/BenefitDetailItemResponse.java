package com.noh.zup.domain.benefit;

public record BenefitDetailItemResponse(
        Long id,
        String brandName,
        String title,
        String description,
        String conditionText,
        String imageUrl,
        Integer displayOrder,
        Boolean isActive
) {
    public static BenefitDetailItemResponse from(BenefitDetailItem item) {
        return new BenefitDetailItemResponse(
                item.getId(),
                item.getBrandName(),
                item.getTitle(),
                item.getDescription(),
                item.getConditionText(),
                item.getImageUrl(),
                item.getDisplayOrder(),
                item.getIsActive()
        );
    }
}
