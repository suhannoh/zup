package com.noh.zup.domain.collection.extract;

import java.util.List;

public record ExtractedText(
        boolean success,
        String text,
        String benefitDetailImageSources,
        List<ExtractedBenefitBlock> blocks,
        String failureReason
) {
    public static ExtractedText success(String text) {
        return success(text, null);
    }

    public static ExtractedText success(String text, String benefitDetailImageSources) {
        return success(text, benefitDetailImageSources, List.of());
    }

    public static ExtractedText success(String text, String benefitDetailImageSources, List<ExtractedBenefitBlock> blocks) {
        return new ExtractedText(true, text, benefitDetailImageSources, blocks == null ? List.of() : blocks, null);
    }

    public static ExtractedText failure(String failureReason) {
        return new ExtractedText(false, null, null, List.of(), failureReason);
    }
}
