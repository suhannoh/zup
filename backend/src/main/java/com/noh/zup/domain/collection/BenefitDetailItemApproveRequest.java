package com.noh.zup.domain.collection;

import jakarta.validation.constraints.Size;

public record BenefitDetailItemApproveRequest(
        @Size(max = 120) String brandName,
        @Size(max = 300) String title,
        @Size(max = 500) String description,
        @Size(max = 500) String conditionText,
        @Size(max = 1000) String imageUrl,
        Integer displayOrder
) {
}
