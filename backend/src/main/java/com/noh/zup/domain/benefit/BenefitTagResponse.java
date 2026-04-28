package com.noh.zup.domain.benefit;

import com.noh.zup.domain.tag.Tag;

public record BenefitTagResponse(
        String name,
        String slug
) {
    public static BenefitTagResponse from(Tag tag) {
        return new BenefitTagResponse(tag.getName(), tag.getSlug());
    }
}
