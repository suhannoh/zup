package com.noh.zup.domain.collection.robots;

import com.noh.zup.domain.collection.fetch.FetchResult;
import com.noh.zup.domain.collection.fetch.OfficialSourceFetcher;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RobotsTxtChecker {

    private final OfficialSourceFetcher officialSourceFetcher;

    public RobotsTxtChecker(OfficialSourceFetcher officialSourceFetcher) {
        this.officialSourceFetcher = officialSourceFetcher;
    }

    public RobotsTxtCheckResult check(String targetUrl) {
        URI targetUri;
        try {
            targetUri = URI.create(targetUrl);
        } catch (IllegalArgumentException exception) {
            return RobotsTxtCheckResult.parseFailed(null, "Invalid source URL: " + exception.getMessage());
        }
        if (!StringUtils.hasText(targetUri.getScheme()) || !StringUtils.hasText(targetUri.getHost())) {
            return RobotsTxtCheckResult.parseFailed(null, "Source URL must include scheme and host");
        }

        String robotsTxtUrl = targetUri.getScheme() + "://" + targetUri.getAuthority() + "/robots.txt";
        FetchResult fetchResult = officialSourceFetcher.fetch(robotsTxtUrl);
        if (!fetchResult.success()) {
            if (fetchResult.statusCode() == 404) {
                return RobotsTxtCheckResult.allowed(robotsTxtUrl, "robots.txt not found");
            }
            return RobotsTxtCheckResult.fetchFailed(
                    robotsTxtUrl,
                    "robots.txt 확인에 실패했습니다: " + fetchResult.failureReason()
            );
        }

        try {
            return evaluate(robotsTxtUrl, targetUri.getPath(), fetchResult.html());
        } catch (RuntimeException exception) {
            return RobotsTxtCheckResult.parseFailed(
                    robotsTxtUrl,
                    "robots.txt 파싱에 실패했습니다: " + exception.getMessage()
            );
        }
    }

    private RobotsTxtCheckResult evaluate(String robotsTxtUrl, String path, String robotsText) {
        List<Rule> rules = parseRules(robotsText);
        return rules.stream()
                .filter(rule -> pathMatches(path, rule.path()))
                .max(Comparator.comparingInt(rule -> rule.path().length()))
                .map(rule -> rule.allow()
                        ? RobotsTxtCheckResult.allowed(robotsTxtUrl, rule.toDisplayString())
                        : RobotsTxtCheckResult.disallowed(robotsTxtUrl, rule.toDisplayString()))
                .orElseGet(() -> RobotsTxtCheckResult.allowed(robotsTxtUrl, "no matching robots.txt rule"));
    }

    private List<Rule> parseRules(String robotsText) {
        List<Rule> rules = new ArrayList<>();
        List<String> currentUserAgents = new ArrayList<>();
        boolean appliesToZupBot = false;
        boolean hasRulesInCurrentGroup = false;

        for (String rawLine : robotsText.split("\\R")) {
            String line = rawLine.replaceAll("#.*$", "").trim();
            if (!StringUtils.hasText(line) || !line.contains(":")) {
                continue;
            }
            String key = line.substring(0, line.indexOf(':')).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(line.indexOf(':') + 1).trim();
            if ("user-agent".equals(key)) {
                if (hasRulesInCurrentGroup) {
                    currentUserAgents.clear();
                    appliesToZupBot = false;
                    hasRulesInCurrentGroup = false;
                }
                currentUserAgents.add(value.toLowerCase(Locale.ROOT));
                appliesToZupBot = currentUserAgents.stream().anyMatch(this::isTargetUserAgent);
                continue;
            }
            if (!appliesToZupBot) {
                continue;
            }
            if ("allow".equals(key) || "disallow".equals(key)) {
                hasRulesInCurrentGroup = true;
                if (!StringUtils.hasText(value)) {
                    continue;
                }
                rules.add(new Rule("allow".equals(key), value));
            }
        }
        return rules;
    }

    private boolean isTargetUserAgent(String userAgent) {
        return "*".equals(userAgent) || userAgent.contains("zup") || userAgent.contains("zupofficialsourcecollector");
    }

    private boolean pathMatches(String targetPath, String rulePath) {
        String path = StringUtils.hasText(targetPath) ? targetPath : "/";
        String normalizedRule = rulePath.trim();
        if (!StringUtils.hasText(normalizedRule)) {
            return false;
        }
        if (normalizedRule.endsWith("$")) {
            return path.equals(normalizedRule.substring(0, normalizedRule.length() - 1));
        }
        return path.startsWith(normalizedRule);
    }

    private record Rule(boolean allow, String path) {
        String toDisplayString() {
            return (allow ? "Allow: " : "Disallow: ") + path;
        }
    }
}
