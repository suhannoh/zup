package com.noh.zup.domain.benefit;

import jakarta.validation.constraints.Size;

public record BenefitDetailItemRequest(
        @Size(max = 120) String brandName,
        @Size(max = 300) String title,
        @Size(max = 500) String description,
        @Size(max = 500) String conditionText,
        @Size(max = 1000) String imageUrl,
        Integer displayOrder
) {
}
