package com.noh.zup.domain.collection.extract;

public record ExtractedImageSource(
        String src,
        String alt,
        String title,
        String ariaLabel
) {
}
