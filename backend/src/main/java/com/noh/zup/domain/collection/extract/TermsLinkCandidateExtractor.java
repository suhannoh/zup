package com.noh.zup.domain.collection.extract;

import com.noh.zup.domain.collection.TermsLinkCandidate;
import com.noh.zup.domain.collection.TermsLinkCandidateConfidence;
import com.noh.zup.domain.collection.TermsLinkCandidateType;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class TermsLinkCandidateExtractor {

    public List<TermsLinkCandidate> extract(String html, String baseUrl) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document document = Jsoup.parse(html, baseUrl);
        Map<String, TermsLinkCandidate> candidatesByUrl = new LinkedHashMap<>();

        for (Element link : document.select("a[href]")) {
            String label = normalizeLabel(link.text());
            String href = link.attr("href");
            String absoluteUrl = normalizeUrl(baseUrl, href);
            if (absoluteUrl == null) {
                continue;
            }
            TermsLinkCandidateType type = classify(label, absoluteUrl);
            if (type == TermsLinkCandidateType.OTHER && label.isBlank()) {
                continue;
            }
            TermsLinkCandidate candidate = new TermsLinkCandidate(
                    label,
                    absoluteUrl,
                    type,
                    confidence(type, label, absoluteUrl)
            );
            candidatesByUrl.putIfAbsent(absoluteUrl, candidate);
        }

        return candidatesByUrl.values().stream()
                .limit(10)
                .toList();
    }

    private String normalizeLabel(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String normalizeUrl(String baseUrl, String href) {
        try {
            URI base = URI.create(baseUrl);
            URI resolved = base.resolve(href).normalize();
            String scheme = resolved.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            return resolved.toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private TermsLinkCandidateType classify(String label, String url) {
        String target = (label + " " + url).toLowerCase(Locale.ROOT);
        if (containsAny(target, "개인정보처리방침", "개인정보 처리방침", "privacy")) {
            return TermsLinkCandidateType.PRIVACY;
        }
        if (containsAny(target, "저작권", "copyright")) {
            return TermsLinkCandidateType.COPYRIGHT;
        }
        if (containsAny(target, "법적고지", "legal")) {
            return TermsLinkCandidateType.LEGAL;
        }
        if (containsAny(target, "이용약관", "서비스 이용약관", "약관", "terms", "terms of service", "terms and conditions")) {
            return TermsLinkCandidateType.TERMS;
        }
        if (containsAny(target, "고객센터", "회사소개", "policy")) {
            return TermsLinkCandidateType.OTHER;
        }
        return TermsLinkCandidateType.OTHER;
    }

    private TermsLinkCandidateConfidence confidence(TermsLinkCandidateType type, String label, String url) {
        String labelLower = label.toLowerCase(Locale.ROOT);
        String urlLower = url.toLowerCase(Locale.ROOT);
        if (type == TermsLinkCandidateType.PRIVACY) {
            return TermsLinkCandidateConfidence.MEDIUM;
        }
        if (labelLower.contains("이용약관")
                || labelLower.contains("terms of service")
                || labelLower.contains("terms and conditions")
                || labelLower.equals("terms")
                || labelLower.contains("법적고지")) {
            return TermsLinkCandidateConfidence.HIGH;
        }
        if (urlLower.contains("terms") || urlLower.contains("legal")) {
            return TermsLinkCandidateConfidence.MEDIUM;
        }
        return TermsLinkCandidateConfidence.LOW;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
