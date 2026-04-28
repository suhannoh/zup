package com.noh.zup.domain.tag;

public record TagResponse(
        Long id,
        String name,
        String slug,
        Integer displayOrder
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                tag.getDisplayOrder()
        );
    }
}
