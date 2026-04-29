package com.noh.zup.domain.collection.extract;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HtmlTextExtractor {

    private static final int MIN_TEXT_LENGTH = 20;
    private static final int MAX_IMAGE_SOURCE_COUNT = 30;
    public static final String BLOCK_SEPARATOR = "\n\n--- ZUP_BLOCK ---\n\n";
    private static final Pattern INLINE_BACKGROUND_IMAGE = Pattern.compile("url\\(['\"]?([^'\")]+)['\"]?\\)");
    private static final Pattern STANDALONE_DISCOUNT = Pattern.compile("^([0-9][0-9,]*\\s*(?:만원|원)?|[0-9]+\\s*%|[0-9]+\\s*퍼센트)\\s*(?:중복\\s*)?할인\\.?$");
    private static final Pattern CONDITION_ONLY = Pattern.compile("^[0-9][0-9,]*\\s*(?:만원|원)?\\s*이상\\s*(구매|주문|결제)\\s*시\\.?$");
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
            "타임캡슐",
            "자물쇠",
            "free",
            "discount",
            "coupon",
            "gift",
            "combo"
    };
    private static final String BLOCK_SELECTOR = String.join(", ",
            "section",
            "article",
            "li",
            "tr",
            "[class*=card]",
            "[class*=coupon]",
            "[class*=benefit]",
            "[class*=item]",
            "[class*=tab]",
            "[class*=accordion]"
    );

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
        String benefitDetailImageSources = extractBenefitDetailImageSources(document, baseUrl);
        List<ExtractedBenefitBlock> blocks = extractBenefitBlocks(document);
        String blockText = blocks.stream()
                .map(block -> cleanText(String.join(" ", block.headingText(), block.text())))
                .filter(StringUtils::hasText)
                .distinct()
                .reduce((left, right) -> left + BLOCK_SEPARATOR + right)
                .orElse(null);
        for (Element element : document.select("h1, h2, h3, h4, h5, h6, p, li, dt, dd, tr, section, article, main, div")) {
            element.appendText("\n");
        }

        String text = document.body() == null ? document.wholeText() : document.body().wholeText();
        String normalized = text
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        String extracted = StringUtils.hasText(blockText) ? blockText : normalized;
        if (extracted.length() < MIN_TEXT_LENGTH) {
            return ExtractedText.failure("Extracted text is too short");
        }

        return ExtractedText.success(extracted, benefitDetailImageSources, blocks);
    }

    private List<ExtractedBenefitBlock> extractBenefitBlocks(Document document) {
        List<ExtractedBenefitBlock> blocks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int index = 1;
        for (Element element : document.select(BLOCK_SELECTOR)) {
            String text = cleanText(element.text());
            if (!StringUtils.hasText(text) || text.length() < 8 || text.length() > 1200) {
                continue;
            }
            if (!isBenefitDetailText(text) && !containsBirthdayHint(text) && !containsConditionHint(text)) {
                continue;
            }
            String key = element.tagName() + ":" + text;
            if (!seen.add(key)) {
                continue;
            }
            blocks.add(new ExtractedBenefitBlock(
                    "block-" + index++,
                    element.tagName(),
                    nearestHeading(element),
                    text,
                    nearbyHeadings(element),
                    imageSources(element),
                    cssClassHints(element)
            ));
        }
        return blocks;
    }

    private boolean containsBirthdayHint(String text) {
        String lower = text.toLowerCase();
        return lower.contains("생일") || lower.contains("birthday") || lower.contains("birth") || lower.contains("생년월일");
    }

    private boolean containsConditionHint(String text) {
        return text.contains("유의사항") || text.contains("사용 조건") || text.contains("제외") || text.contains("중복") || text.contains("발급");
    }

    private String nearestHeading(Element element) {
        Element current = element;
        int depth = 0;
        while (current != null && depth < 4) {
            Element heading = current.selectFirst("h1, h2, h3, h4, h5, h6, [class*=title], [class*=heading]");
            if (heading != null && StringUtils.hasText(heading.text())) {
                return cleanText(heading.text());
            }
            current = current.parent();
            depth++;
        }
        Element previous = element.previousElementSibling();
        depth = 0;
        while (previous != null && depth < 5) {
            if (previous.is("h1, h2, h3, h4, h5, h6") && StringUtils.hasText(previous.text())) {
                return cleanText(previous.text());
            }
            previous = previous.previousElementSibling();
            depth++;
        }
        return "";
    }

    private List<String> nearbyHeadings(Element element) {
        List<String> headings = new ArrayList<>();
        String nearest = nearestHeading(element);
        if (StringUtils.hasText(nearest)) {
            headings.add(nearest);
        }
        Element parent = element.parent();
        int depth = 0;
        while (parent != null && depth < 3) {
            for (Element heading : parent.select("> h1, > h2, > h3, > h4, > h5, > h6")) {
                String text = cleanText(heading.text());
                if (StringUtils.hasText(text) && !headings.contains(text)) {
                    headings.add(text);
                }
            }
            parent = parent.parent();
            depth++;
        }
        return headings;
    }

    private List<ExtractedImageSource> imageSources(Element element) {
        return element.select("img").stream()
                .limit(5)
                .map(image -> new ExtractedImageSource(
                        resolveImageSrc(image, ""),
                        cleanText(image.attr("alt")),
                        cleanText(image.attr("title")),
                        cleanText(image.attr("aria-label"))
                ))
                .toList();
    }

    private List<String> cssClassHints(Element element) {
        List<String> hints = new ArrayList<>();
        if (StringUtils.hasText(element.className())) {
            hints.add(element.className());
        }
        if (StringUtils.hasText(element.id())) {
            hints.add("#" + element.id());
        }
        return hints;
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

    private String extractBenefitDetailImageSources(Document document, String baseUrl) {
        StringBuilder builder = new StringBuilder();
        Set<String> seen = new HashSet<>();
        int count = 0;

        for (Element element : document.select("p, li, dd, dt, td, th, div, article, section")) {
            if (count >= MAX_IMAGE_SOURCE_COUNT) {
                break;
            }
            if (element.select("img").size() > 1) {
                continue;
            }
            String nearbyText = bestNearbyText(element);
            if (!isBenefitDetailText(nearbyText)) {
                continue;
            }

            Element image = findNearbyImage(element);
            String src = image == null ? findInlineBackgroundImage(element, baseUrl) : resolveImageSrc(image, baseUrl);
            if (!StringUtils.hasText(src)) {
                continue;
            }
            String key = src;
            if (!seen.add(key)) {
                continue;
            }

            Element parentLink = image == null ? element.closest("a") : image.closest("a");
            appendImageSource(builder, nearbyText, src, image, parentLink);
            count++;
        }
        return builder.length() == 0 ? null : builder.toString().trim();
    }

    private boolean isBenefitDetailText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String keyword : BENEFIT_DETAIL_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String bestNearbyText(Element element) {
        String current = cleanText(element.text());
        Element parent = element.parent();
        if (parent == null) {
            return current;
        }
        String parentText = cleanText(parent.text());
        if (StringUtils.hasText(parentText)
                && parentText.length() <= 250
                && isBenefitDetailText(parentText)
                && (isIncompleteBenefitText(current) || parent.select("img").size() > 0)) {
            return parentText;
        }
        return current;
    }

    private boolean isIncompleteBenefitText(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String cleaned = cleanCouponPrefix(text);
        return STANDALONE_DISCOUNT.matcher(cleaned).matches()
                || CONDITION_ONLY.matcher(cleaned).matches()
                || cleaned.matches(".*(콤보|세트|상품|주문|구매|결제).*시\\.?$");
    }

    private Element findNearbyImage(Element element) {
        Element image = firstImage(element.select("img"));
        if (image != null) {
            return image;
        }
        Element parent = element.parent();
        int depth = 0;
        while (parent != null && depth < 3) {
            image = firstImage(parent.select("img"));
            if (image != null) {
                return image;
            }
            parent = parent.parent();
            depth++;
        }
        Element previous = element.previousElementSibling();
        depth = 0;
        while (previous != null && depth < 4) {
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

    private String resolveImageSrc(Element image, String baseUrl) {
        String src = firstText(
                image.absUrl("src"),
                image.absUrl("data-src"),
                image.absUrl("data-original"),
                image.absUrl("data-lazy-src")
        );
        if (!StringUtils.hasText(src)) {
            src = firstText(
                    image.attr("src"),
                    image.attr("data-src"),
                    image.attr("data-original"),
                    image.attr("data-lazy-src")
            );
        }
        if (!StringUtils.hasText(src) && StringUtils.hasText(image.attr("srcset"))) {
            src = firstSrcsetUrl(image.attr("srcset"));
        }
        return absolutize(src, baseUrl);
    }

    private String findInlineBackgroundImage(Element element, String baseUrl) {
        Element current = element;
        int depth = 0;
        while (current != null && depth < 3) {
            String style = current.attr("style");
            Matcher matcher = INLINE_BACKGROUND_IMAGE.matcher(style);
            if (matcher.find()) {
                return absolutize(matcher.group(1), baseUrl);
            }
            current = current.parent();
            depth++;
        }
        return null;
    }

    private String firstSrcsetUrl(String srcset) {
        if (!StringUtils.hasText(srcset)) {
            return null;
        }
        String first = srcset.split(",")[0].trim();
        if (!StringUtils.hasText(first)) {
            return null;
        }
        return first.split("\\s+")[0].trim();
    }

    private String absolutize(String src, String baseUrl) {
        if (!StringUtils.hasText(src)) {
            return null;
        }
        String trimmed = src.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || !StringUtils.hasText(baseUrl)) {
            return trimmed;
        }
        try {
            return URI.create(baseUrl).resolve(trimmed).toString();
        } catch (IllegalArgumentException ignored) {
            return trimmed;
        }
    }

    private void appendImageSource(StringBuilder builder, String nearbyText, String src, Element image, Element parentLink) {
        String alt = image == null ? "" : cleanText(image.attr("alt"));
        String title = image == null ? "" : cleanText(image.attr("title"));
        String ariaLabel = image == null ? "" : cleanText(image.attr("aria-label"));
        String className = image == null ? "" : cleanText(image.className());
        String parentHref = parentLink == null ? "" : parentLink.absUrl("href");
        if (!StringUtils.hasText(parentHref) && parentLink != null) {
            parentHref = parentLink.attr("href");
        }
        String parentTitle = parentLink == null ? "" : cleanText(parentLink.attr("title"));
        String parentAriaLabel = parentLink == null ? "" : cleanText(parentLink.attr("aria-label"));
        String possibleBrandName = possibleBrandName(alt, title, ariaLabel, parentTitle, parentAriaLabel);
        String confidence = StringUtils.hasText(possibleBrandName) ? "high" : "low";

        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append("쿠폰: ").append(cleanCouponPrefix(nearbyText)).append("\n");
        builder.append("imgSrc: ").append(src).append("\n");
        builder.append("imgAlt: ").append(alt).append("\n");
        builder.append("imgTitle: ").append(title).append("\n");
        builder.append("imgAriaLabel: ").append(ariaLabel).append("\n");
        builder.append("parentHref: ").append(parentHref).append("\n");
        builder.append("parentTitle: ").append(parentTitle).append("\n");
        builder.append("parentAriaLabel: ").append(parentAriaLabel).append("\n");
        builder.append("nearbyText: ").append(truncate(cleanCouponPrefix(nearbyText), 200)).append("\n");
        builder.append("className: ").append(className).append("\n");
        builder.append("possibleBrandName: ").append(possibleBrandName).append("\n");
        builder.append("confidence: ").append(confidence);
    }

    private String possibleBrandName(String... values) {
        for (String value : values) {
            if (looksLikeBrandName(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean looksLikeBrandName(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();
        if (trimmed.length() > 40) {
            return false;
        }
        return !lower.contains("logo")
                && !lower.contains("쿠폰")
                && !lower.contains("할인")
                && !lower.contains("무료")
                && !lower.contains("증정")
                && !lower.contains("이미지")
                && !lower.contains("icon");
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String cleanCouponPrefix(String text) {
        return cleanText(text).replaceAll("^\\[[^]]*]\\s*", "");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
