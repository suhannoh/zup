package com.noh.zup.domain.collection.extract;

import java.util.List;

public record ExtractedBenefitBlock(
        String blockId,
        String tagName,
        String headingText,
        String text,
        List<String> nearbyHeadings,
        List<ExtractedImageSource> imageSources,
        List<String> cssClassHints
) {
}
