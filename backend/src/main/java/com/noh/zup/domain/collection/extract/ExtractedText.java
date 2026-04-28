package com.noh.zup.domain.collection.extract;

public record ExtractedText(
        boolean success,
        String text,
        String failureReason
) {
    public static ExtractedText success(String text) {
        return new ExtractedText(true, text, null);
    }

    public static ExtractedText failure(String failureReason) {
        return new ExtractedText(false, null, failureReason);
    }
}
