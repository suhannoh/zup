package com.noh.zup.domain.collection.extract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HtmlTextExtractor {

    private static final int MIN_TEXT_LENGTH = 20;
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

    public ExtractedText extract(String html) {
        if (!StringUtils.hasText(html)) {
            return ExtractedText.failure("HTML is empty");
        }

        Document document = Jsoup.parse(html);
        document.select(STRUCTURAL_NOISE_SELECTOR).remove();
        removeNoiseAttributeElements(document);
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

        return ExtractedText.success(normalized);
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
}
