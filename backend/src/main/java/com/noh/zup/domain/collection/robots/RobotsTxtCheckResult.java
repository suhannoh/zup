package com.noh.zup.domain.collection.robots;

public record RobotsTxtCheckResult(
        boolean allowed,
        String failureReason,
        String message,
        String robotsTxtUrl,
        String matchedRule
) {
    public static RobotsTxtCheckResult allowed(String robotsTxtUrl, String matchedRule) {
        return new RobotsTxtCheckResult(true, null, "robots.txt collection allowed", robotsTxtUrl, matchedRule);
    }

    public static RobotsTxtCheckResult disallowed(String robotsTxtUrl, String matchedRule) {
        return new RobotsTxtCheckResult(
                false,
                "ROBOTS_TXT_DISALLOWED",
                "robots.txt 정책에 의해 수집이 차단되었습니다.",
                robotsTxtUrl,
                matchedRule
        );
    }

    public static RobotsTxtCheckResult fetchFailed(String robotsTxtUrl, String message) {
        return new RobotsTxtCheckResult(
                false,
                "ROBOTS_TXT_FETCH_FAILED",
                message,
                robotsTxtUrl,
                null
        );
    }

    public static RobotsTxtCheckResult parseFailed(String robotsTxtUrl, String message) {
        return new RobotsTxtCheckResult(
                false,
                "ROBOTS_TXT_PARSE_FAILED",
                message,
                robotsTxtUrl,
                null
        );
    }
}
