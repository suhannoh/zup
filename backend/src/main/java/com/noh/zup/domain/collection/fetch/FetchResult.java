package com.noh.zup.domain.collection.fetch;

public record FetchResult(
        boolean success,
        int statusCode,
        String html,
        String failureReason
) {
    public static FetchResult success(int statusCode, String html) {
        return new FetchResult(true, statusCode, html, null);
    }

    public static FetchResult failure(int statusCode, String failureReason) {
        return new FetchResult(false, statusCode, null, failureReason);
    }
}
