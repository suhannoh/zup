package com.noh.zup.domain.tag;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminTagUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 120) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        Integer displayOrder,
        Boolean isActive
) {
}
