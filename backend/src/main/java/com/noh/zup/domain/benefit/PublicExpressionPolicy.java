package com.noh.zup.domain.benefit;

import java.util.List;
import org.springframework.util.StringUtils;

public final class PublicExpressionPolicy {

    private static final List<String> PROHIBITED_TERMS = List.of(
            "공식 쿠폰",
            "공식 제휴",
            "제휴 혜택",
            "인증 혜택",
            "보장",
            "확정",
            "무조건",
            "반드시 제공",
            "Zup 단독",
            "최신 보장",
            "100% 사용 가능"
    );

    private PublicExpressionPolicy() {
    }

    public static List<String> findProhibitedTerms(Benefit benefit) {
        String text = String.join("\n",
                value(benefit.getTitle()),
                value(benefit.getSummary()),
                value(benefit.getDetail()),
                value(benefit.getConditionSummary()),
                value(benefit.getUsagePeriodDescription()),
                value(benefit.getCaution())
        );
        return PROHIBITED_TERMS.stream()
                .filter(term -> StringUtils.hasText(text) && text.contains(term))
                .toList();
    }

    public static boolean containsProhibitedTerms(Benefit benefit) {
        return !findProhibitedTerms(benefit).isEmpty();
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
