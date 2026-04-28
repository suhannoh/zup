package com.noh.zup.domain.tag;

import java.time.LocalDateTime;

public record AdminTagResponse(
        Long id,
        String name,
        String slug,
        Integer displayOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminTagResponse from(Tag tag) {
        return new AdminTagResponse(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                tag.getDisplayOrder(),
                tag.getIsActive(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }
}
