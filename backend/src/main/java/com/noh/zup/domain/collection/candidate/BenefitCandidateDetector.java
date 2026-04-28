package com.noh.zup.domain.collection.candidate;

import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import com.noh.zup.domain.collection.BenefitCandidate;
import com.noh.zup.domain.collection.BenefitCandidateRepository;
import com.noh.zup.domain.collection.PageSnapshot;
import com.noh.zup.domain.collection.SourceWatch;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BenefitCandidateDetector {

    private static final List<String> BENEFIT_KEYWORDS = List.of(
            "쿠폰", "coupon", "혜택", "할인", "discount", "무료", "gift", "benefit"
    );
    private static final List<String> APP_MEMBERSHIP_KEYWORDS = List.of("앱", "멤버십", "회원");

    private final BenefitCandidateRepository benefitCandidateRepository;

    public BenefitCandidateDetector(BenefitCandidateRepository benefitCandidateRepository) {
        this.benefitCandidateRepository = benefitCandidateRepository;
    }

    public List<BenefitCandidate> detect(SourceWatch sourceWatch, PageSnapshot snapshot) {
        String text = snapshot.getExtractedText();
        String lowerText = text.toLowerCase(Locale.ROOT);
        if (!containsBirthdayKeyword(lowerText) || BENEFIT_KEYWORDS.stream().noneMatch(lowerText::contains)) {
            return List.of();
        }

        String evidenceText = extractEvidenceText(text);
        if (!StringUtils.hasText(evidenceText)) {
            return List.of();
        }
        if (benefitCandidateRepository.existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(
                sourceWatch.getId(),
                snapshot.getContentHash(),
                evidenceText
        )) {
            return List.of();
        }

        BenefitCandidate candidate = new BenefitCandidate(
                sourceWatch.getBrand(),
                sourceWatch,
                snapshot,
                sourceWatch.getBrand().getName() + " 생일 혜택 후보",
                truncate(evidenceText, 500),
                detectBenefitType(lowerText),
                OccasionType.BIRTHDAY,
                BirthdayTimingType.UNKNOWN,
                lowerText.contains("앱"),
                lowerText.contains("회원가입") || lowerText.contains("가입") || lowerText.contains("signup"),
                lowerText.contains("멤버십") || lowerText.contains("회원") || lowerText.contains("membership"),
                evidenceText,
                calculateConfidence(sourceWatch, lowerText)
        );
        return List.of(benefitCandidateRepository.save(candidate));
    }

    private boolean containsBirthdayKeyword(String lowerText) {
        return lowerText.contains("생일") || lowerText.contains("birthday");
    }

    private String extractEvidenceText(String text) {
        String[] sentences = text.split("(?<=[.!?。！？])\\s+|\\n+");
        List<String> evidenceSentences = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            String lowerSentence = sentence.toLowerCase(Locale.ROOT);
            if (lowerSentence.contains("생일") || lowerSentence.contains("birthday")) {
                int start = Math.max(0, i - 1);
                int end = Math.min(sentences.length - 1, i + 1);
                for (int j = start; j <= end && evidenceSentences.size() < 3; j++) {
                    String candidate = sentences[j].trim();
                    if (StringUtils.hasText(candidate)) {
                        evidenceSentences.add(candidate);
                    }
                }
                break;
            }
        }

        if (evidenceSentences.isEmpty()) {
            int index = firstBirthdayIndex(text);
            if (index < 0) {
                return null;
            }
            int start = Math.max(0, index - 160);
            int end = Math.min(text.length(), index + 240);
            return truncate(text.substring(start, end).trim(), 1000);
        }
        return truncate(String.join(" ", evidenceSentences), 1000);
    }

    private int firstBirthdayIndex(String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        int koreanIndex = lowerText.indexOf("생일");
        int englishIndex = lowerText.indexOf("birthday");
        if (koreanIndex < 0) {
            return englishIndex;
        }
        if (englishIndex < 0) {
            return koreanIndex;
        }
        return Math.min(koreanIndex, englishIndex);
    }

    private BenefitType detectBenefitType(String lowerText) {
        if (lowerText.contains("쿠폰") || lowerText.contains("coupon")) {
            return BenefitType.COUPON;
        }
        if (lowerText.contains("할인") || lowerText.contains("discount")) {
            return BenefitType.DISCOUNT;
        }
        if (lowerText.contains("무료")) {
            return BenefitType.FREE_ITEM;
        }
        if (lowerText.contains("gift")) {
            return BenefitType.GIFT;
        }
        return BenefitType.ETC;
    }

    private BigDecimal calculateConfidence(SourceWatch sourceWatch, String lowerText) {
        double confidence = 0.5;
        if ((lowerText.contains("생일") || lowerText.contains("birthday"))
                && (lowerText.contains("쿠폰") || lowerText.contains("coupon"))) {
            confidence += 0.2;
        }
        if (sourceWatch.getSourceType().name().startsWith("OFFICIAL")) {
            confidence += 0.2;
        }
        if (APP_MEMBERSHIP_KEYWORDS.stream().anyMatch(lowerText::contains)) {
            confidence += 0.1;
        }
        return BigDecimal.valueOf(Math.min(confidence, 1.0));
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
