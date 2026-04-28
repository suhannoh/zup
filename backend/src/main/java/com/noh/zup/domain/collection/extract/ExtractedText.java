package com.noh.zup.domain.collection.extract;

public record ExtractedText(
        boolean success,
        String text,
        String benefitDetailImageSources,
        String failureReason
) {
    public static ExtractedText success(String text) {
        return success(text, null);
    }

    public static ExtractedText success(String text, String benefitDetailImageSources) {
        return new ExtractedText(true, text, benefitDetailImageSources, null);
    }

    public static ExtractedText failure(String failureReason) {
        return new ExtractedText(false, null, null, failureReason);
    }
}
