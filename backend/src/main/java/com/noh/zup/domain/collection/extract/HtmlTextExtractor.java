package com.noh.zup.domain.collection.extract;

import java.util.HashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HtmlTextExtractor {

    private static final int MIN_TEXT_LENGTH = 20;
    private static final int MAX_IMAGE_SOURCE_COUNT = 20;
    private static final String STRUCTURAL_NOISE_SELECTOR = String.join(", ",
            "script",
            "style",
            "noscript",
            "header",
            "footer",
            "nav",
            "aside",
            "form",
            "button",
            "svg",
            "iframe"
    );
    private static final String[] NOISE_ATTRIBUTE_KEYWORDS = {
            "menu",
            "gnb",
            "lnb",
            "nav",
            "footer",
            "header",
            "breadcrumb",
            "sidebar",
            "sns",
            "social"
    };
    private static final String[] BENEFIT_DETAIL_KEYWORDS = {
            "할인",
            "무료",
            "증정",
            "쿠폰",
            "원",
            "%",
            "퍼센트",
            "콤보",
            "세트",
            "샐러드",
            "free",
            "discount",
            "coupon",
            "gift",
            "combo"
    };

    public ExtractedText extract(String html) {
        return extract(html, "");
    }

    public ExtractedText extract(String html, String baseUrl) {
        if (!StringUtils.hasText(html)) {
            return ExtractedText.failure("HTML is empty");
        }

        Document document = Jsoup.parse(html, baseUrl == null ? "" : baseUrl);
        document.select(STRUCTURAL_NOISE_SELECTOR).remove();
        removeNoiseAttributeElements(document);
        String benefitDetailImageSources = extractBenefitDetailImageSources(document);
        for (Element element : document.select("h1, h2, h3, h4, h5, h6, p, li, dt, dd, tr, section, article, main, div")) {
            element.appendText("\n");
        }

        String text = document.body() == null ? document.wholeText() : document.body().wholeText();
        String normalized = text
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (normalized.length() < MIN_TEXT_LENGTH) {
            return ExtractedText.failure("Extracted text is too short");
        }

        return ExtractedText.success(normalized, benefitDetailImageSources);
    }

    private void removeNoiseAttributeElements(Document document) {
        for (Element element : document.getAllElements()) {
            if ("html".equals(element.tagName()) || "body".equals(element.tagName()) || "main".equals(element.tagName())) {
                continue;
            }
            String id = element.id().toLowerCase();
            String className = element.className().toLowerCase();
            String ariaLabel = element.attr("aria-label").toLowerCase();
            String combined = id + " " + className + " " + ariaLabel;
            for (String keyword : NOISE_ATTRIBUTE_KEYWORDS) {
                if (combined.contains(keyword)) {
                    element.remove();
                    break;
                }
            }
        }
    }

    private String extractBenefitDetailImageSources(Document document) {
        StringBuilder builder = new StringBuilder();
        Set<String> seen = new HashSet<>();
        int count = 0;
        for (Element element : document.select("p, li, dd, dt, td, th, div")) {
            if (count >= MAX_IMAGE_SOURCE_COUNT) {
                break;
            }
            String benefitText = cleanText(element.ownText());
            if (!isBenefitDetailText(benefitText)) {
                continue;
            }

            Element image = findNearbyImage(element);
            if (image == null) {
                continue;
            }
            String src = image.absUrl("src");
            if (!StringUtils.hasText(src)) {
                src = image.attr("src").trim();
            }
            if (!StringUtils.hasText(src)) {
                continue;
            }
            String key = cleanCouponPrefix(benefitText) + "|" + src;
            if (!seen.add(key)) {
                continue;
            }
            appendImageSource(builder, benefitText, src, image.attr("alt"), image.attr("title"));
            count++;
        }
        return builder.length() == 0 ? null : builder.toString().trim();
    }

    private boolean isBenefitDetailText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        if (!lowerText.contains("생일") && !lowerText.contains("birthday")
                && !lowerText.contains("할인") && !lowerText.contains("무료") && !lowerText.contains("증정")) {
            return false;
        }
        for (String keyword : BENEFIT_DETAIL_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Element findNearbyImage(Element element) {
        Element image = firstImage(element.select("img"));
        if (image != null) {
            return image;
        }
        Element parent = element.parent();
        if (parent != null) {
            image = firstImage(parent.select("img"));
            if (image != null) {
                return image;
            }
        }
        Element previous = element.previousElementSibling();
        int depth = 0;
        while (previous != null && depth < 3) {
            image = firstImage(previous.select("img"));
            if (image != null) {
                return image;
            }
            if ("img".equals(previous.tagName())) {
                return previous;
            }
            previous = previous.previousElementSibling();
            depth++;
        }
        return null;
    }

    private Element firstImage(Elements images) {
        return images.isEmpty() ? null : images.first();
    }

    private void appendImageSource(StringBuilder builder, String benefitText, String src, String alt, String title) {
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append("쿠폰: ").append(cleanCouponPrefix(benefitText)).append("\n");
        builder.append("imgSrc: ").append(src).append("\n");
        builder.append("imgAlt: ").append(StringUtils.hasText(alt) ? alt.trim() : "").append("\n");
        builder.append("imgTitle: ").append(StringUtils.hasText(title) ? title.trim() : "");
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String cleanCouponPrefix(String text) {
        return cleanText(text).replaceAll("^\\[[^]]*]\\s*", "");
    }
}
