package com.noh.zup.common.request;

import jakarta.validation.constraints.NotNull;

public record ActiveUpdateRequest(
        @NotNull Boolean isActive
) {
}
