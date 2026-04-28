package com.noh.zup.domain.benefit;

import jakarta.validation.constraints.NotNull;

public record AdminBenefitTagRequest(
        @NotNull Long tagId
) {
}
