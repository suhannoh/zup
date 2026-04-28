package com.noh.zup.domain.brand;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminBrandUpdateRequest(
        Long categoryId,
        @Size(max = 120) String name,
        @Size(max = 150) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        @Size(max = 500) String description,
        @Size(max = 500) String officialUrl,
        @Size(max = 500) String membershipUrl,
        @Size(max = 500) String appUrl,
        @Size(max = 30) String brandColor,
        @Size(max = 500) String logoUrl,
        Boolean isActive
) {
}
