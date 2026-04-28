package com.noh.zup.domain.collection.extract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HtmlTextExtractor {

    private static final int MIN_TEXT_LENGTH = 20;

    public ExtractedText extract(String html) {
        if (!StringUtils.hasText(html)) {
            return ExtractedText.failure("HTML is empty");
        }

        Document document = Jsoup.parse(html);
        document.select("script, style, nav, footer, noscript").remove();

        String text = document.body() == null ? document.text() : document.body().text();
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() < MIN_TEXT_LENGTH) {
            return ExtractedText.failure("Extracted text is too short");
        }

        return ExtractedText.success(normalized);
    }
}
