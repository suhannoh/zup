package com.noh.zup.domain.collection;

import com.noh.zup.domain.source.SourceType;
import jakarta.validation.constraints.Size;

public record SourceWatchUpdateRequest(
        SourceType sourceType,
        @Size(max = 300) String title,
        @Size(max = 1000) String url,
        Boolean isActive
) {
}
