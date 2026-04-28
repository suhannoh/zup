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
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BenefitCandidateDetector {

    private static final List<String> BENEFIT_KEYWORDS = List.of(
            "쿠폰", "coupon", "혜택", "할인", "discount", "무료", "gift", "benefit", "증정", "포인트", "적립"
    );
    private static final List<String> APP_MEMBERSHIP_KEYWORDS = List.of("앱", "멤버십", "회원");
    private static final List<String> RELATED_KEYWORDS = List.of(
            "생일", "birthday", "쿠폰", "coupon", "혜택", "benefit", "할인", "무료", "gift", "회원", "멤버십"
    );
    private static final List<String> MENU_NOISE_KEYWORDS = List.of(
            "개인정보처리방침", "이용약관", "고객센터", "전체메뉴", "닫기", "로그인", "매장 찾기",
            "네이버블로그", "인스타그램", "유튜브", "facebook", "instagram", "youtube", "footer", "navigation"
    );
    private static final List<String> DETAIL_KEYWORDS = List.of(
            "할인", "무료", "증정", "쿠폰", "포인트", "적립", "원", "%", "퍼센트", "1+1", "콤보", "세트",
            "아메리카노", "음료", "케이크", "디저트", "입장권", "관람권", "샐러드", "프렌치프라이", "나초칩",
            "타임캡슐", "자물쇠", "free", "discount", "coupon", "point", "gift", "combo"
    );
    private static final List<String> USAGE_GUIDE_KEYWORDS = List.of(
            "이용안내", "이용 가능", "회원만", "현금", "교환", "양도", "발급", "지급", "1년에 1번",
            "추가 발급", "생년월일", "생일 정보", "회원정보", "사용 조건", "최소 구매", "유효기간", "변경", "제외"
    );
    private static final Pattern MONEY_OR_PERCENT = Pattern.compile("([0-9][0-9,]*\\s*원|[0-9]+\\s*%|[0-9]+\\s*퍼센트)");
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?。！？])\\s+|(?<=다\\.)\\s*|(?<=요\\.)\\s*|(?<=니다\\.)\\s*|\\n+");
    private static final int MAX_SENTENCE_LENGTH = 300;
    private static final int MAX_EVIDENCE_LENGTH = 500;
    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final int MAX_BENEFIT_DETAIL_COUNT = 10;
    private static final int MAX_USAGE_GUIDE_LENGTH = 700;

    private final BenefitCandidateRepository benefitCandidateRepository;

    public BenefitCandidateDetector(BenefitCandidateRepository benefitCandidateRepository) {
        this.benefitCandidateRepository = benefitCandidateRepository;
    }

    public List<BenefitCandidate> detect(SourceWatch sourceWatch, PageSnapshot snapshot) {
        List<BenefitCandidate> createdCandidates = new ArrayList<>();
        detect(sourceWatch, snapshot, createdCandidates);
        return createdCandidates;
    }

    public BenefitCandidateDetectionResult detectWithResult(SourceWatch sourceWatch, PageSnapshot snapshot) {
        return detect(sourceWatch, snapshot, new ArrayList<>());
    }

    private BenefitCandidateDetectionResult detect(
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            List<BenefitCandidate> createdCandidates
    ) {
        String text = snapshot.getExtractedText();
        String lowerText = text.toLowerCase(Locale.ROOT);
        if (!containsBirthdayKeyword(lowerText) || BENEFIT_KEYWORDS.stream().noneMatch(lowerText::contains)) {
            return new BenefitCandidateDetectionResult(0, 0);
        }

        String evidenceText = extractEvidenceText(text);
        if (!StringUtils.hasText(evidenceText)) {
            return new BenefitCandidateDetectionResult(0, 0);
        }
        List<String> sentences = splitSentences(text);
        List<String> benefitDetails = extractBenefitDetails(sentences);
        String benefitDetailText = joinLines(benefitDetails);
        String usageGuideText = extractUsageGuideText(sentences);
        String lowerEvidenceText = evidenceText.toLowerCase(Locale.ROOT);
        String detailAndEvidenceText = (benefitDetailText == null ? "" : benefitDetailText + " ") + evidenceText;
        String lowerDetailAndEvidenceText = detailAndEvidenceText.toLowerCase(Locale.ROOT);
        if (benefitCandidateRepository.existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(
                sourceWatch.getId(),
                snapshot.getContentHash(),
                evidenceText
        )) {
            return new BenefitCandidateDetectionResult(0, 1);
        }

        BenefitCandidate candidate = new BenefitCandidate(
                sourceWatch.getBrand(),
                sourceWatch,
                snapshot,
                buildTitle(sourceWatch.getBrand().getName(), lowerDetailAndEvidenceText, benefitDetails),
                buildSummary(sourceWatch.getBrand().getName(), benefitDetails, evidenceText, lowerEvidenceText),
                detectBenefitType(lowerDetailAndEvidenceText + " " + lowerText),
                OccasionType.BIRTHDAY,
                BirthdayTimingType.UNKNOWN,
                lowerText.contains("앱"),
                lowerText.contains("회원가입") || lowerText.contains("가입") || lowerText.contains("signup"),
                lowerText.contains("멤버십") || lowerText.contains("회원") || lowerText.contains("membership"),
                evidenceText,
                benefitDetailText,
                snapshot.getBenefitDetailImageSources(),
                usageGuideText,
                calculateConfidence(sourceWatch, lowerText)
        );
        createdCandidates.add(benefitCandidateRepository.save(candidate));
        return new BenefitCandidateDetectionResult(1, 0);
    }

    private boolean containsBirthdayKeyword(String lowerText) {
        return lowerText.contains("생일") || lowerText.contains("birthday");
    }

    private String extractEvidenceText(String text) {
        List<String> sentences = splitSentences(text);
        List<String> evidenceSentences = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i).trim();
            String lowerSentence = sentence.toLowerCase(Locale.ROOT);
            if (lowerSentence.contains("생일") || lowerSentence.contains("birthday")) {
                int start = Math.max(0, i - 1);
                int end = Math.min(sentences.size() - 1, i + 1);
                for (int j = start; j <= end && evidenceSentences.size() < 3; j++) {
                    String candidate = sentences.get(j).trim();
                    if (isUsefulEvidenceSentence(candidate)) {
                        evidenceSentences.add(candidate);
                    }
                }
                if (evidenceSentences.isEmpty() && isUsefulEvidenceSentence(sentence)) {
                    evidenceSentences.add(sentence);
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
            return truncate(text.substring(start, end).trim(), MAX_EVIDENCE_LENGTH);
        }
        return truncate(String.join(" ", evidenceSentences), MAX_EVIDENCE_LENGTH);
    }

    private List<String> extractBenefitDetails(List<String> sentences) {
        return sentences.stream()
                .map(String::trim)
                .filter(this::isNotMenuNoise)
                .filter(this::isBenefitDetailSentence)
                .map(this::cleanCouponPrefix)
                .sorted((left, right) -> Integer.compare(benefitPriority(right), benefitPriority(left)))
                .distinct()
                .limit(MAX_BENEFIT_DETAIL_COUNT)
                .toList();
    }

    private boolean isBenefitDetailSentence(String sentence) {
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        if (lowerSentence.contains("이용약관") || lowerSentence.contains("개인정보처리방침")) {
            return false;
        }
        boolean guideOnly = USAGE_GUIDE_KEYWORDS.stream().anyMatch(lowerSentence::contains)
                && !MONEY_OR_PERCENT.matcher(sentence).find()
                && !lowerSentence.contains("할인")
                && !lowerSentence.contains("무료")
                && !lowerSentence.contains("증정")
                && !lowerSentence.contains("free")
                && !lowerSentence.contains("discount")
                && !lowerSentence.contains("gift");
        if (guideOnly) {
            return false;
        }
        return DETAIL_KEYWORDS.stream().anyMatch(lowerSentence::contains);
    }

    private int benefitPriority(String sentence) {
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        int priority = 0;
        if (MONEY_OR_PERCENT.matcher(sentence).find()) {
            priority += 8;
        }
        if (lowerSentence.contains("무료") || lowerSentence.contains("증정") || lowerSentence.contains("free") || lowerSentence.contains("gift")) {
            priority += 6;
        }
        if (lowerSentence.contains("쿠폰") || lowerSentence.contains("coupon")) {
            priority += 4;
        }
        if (lowerSentence.matches(".*[A-Za-z가-힣0-9]+.*(할인|무료|증정|쿠폰|free|discount|gift).*")) {
            priority += 2;
        }
        return priority;
    }

    private String extractUsageGuideText(List<String> sentences) {
        List<String> guides = new ArrayList<>();
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase(Locale.ROOT);
            if (lowerSentence.contains("가입축하") && !containsBirthdayKeyword(lowerSentence)
                    && !lowerSentence.contains("현금") && !lowerSentence.contains("양도")) {
                continue;
            }
            if (USAGE_GUIDE_KEYWORDS.stream().anyMatch(lowerSentence::contains) && isUsefulGuideSentence(sentence)) {
                guides.add(sentence);
            }
        }
        if (guides.isEmpty()) {
            return null;
        }
        String guideText = String.join(" ", guides.stream().distinct().toList());
        return truncate(guideText, MAX_USAGE_GUIDE_LENGTH);
    }

    private boolean isUsefulGuideSentence(String sentence) {
        if (!StringUtils.hasText(sentence)) {
            return false;
        }
        return isNotMenuNoise(sentence);
    }

    private List<String> splitSentences(String text) {
        String normalized = text
                .replaceAll("(?<=다)\\s+", "다.\n")
                .replaceAll("(?<=요)\\s+", "요.\n")
                .replaceAll("(?<=니다)\\s+", "니다.\n");
        String[] rawSentences = SENTENCE_BOUNDARY.split(normalized);
        List<String> sentences = new ArrayList<>();
        for (String rawSentence : rawSentences) {
            String sentence = rawSentence.replaceAll("\\s+", " ").trim();
            if (sentence.length() < 8) {
                continue;
            }
            sentences.add(truncate(sentence, MAX_SENTENCE_LENGTH));
        }
        return sentences;
    }

    private boolean isUsefulEvidenceSentence(String sentence) {
        if (!StringUtils.hasText(sentence)) {
            return false;
        }
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        boolean related = RELATED_KEYWORDS.stream().anyMatch(lowerSentence::contains);
        if (!related) {
            return false;
        }
        if (!isNotMenuNoise(sentence)) {
            return false;
        }
        long relatedKeywordCount = RELATED_KEYWORDS.stream()
                .filter(lowerSentence::contains)
                .count();
        int spaceCount = sentence.length() - sentence.replace(" ", "").length();
        return !(sentence.length() > 120 && spaceCount < 4 && relatedKeywordCount < 2);
    }

    private boolean isNotMenuNoise(String sentence) {
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        long menuKeywordCount = MENU_NOISE_KEYWORDS.stream()
                .filter(lowerSentence::contains)
                .count();
        return menuKeywordCount < 2;
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

    private String buildTitle(String brandName, String lowerText, List<String> benefitDetails) {
        if (lowerText.contains("10종") && (lowerText.contains("쿠폰") || lowerText.contains("coupon"))) {
            return brandName + " 생일축하 10종 쿠폰";
        }
        if (lowerText.contains("50%") || lowerText.contains("50 %")) {
            return brandName + " 생일 50% 할인";
        }
        String freeItem = findFreeItem(benefitDetails);
        if (freeItem != null) {
            return brandName + " 생일 무료 " + freeItem;
        }
        if (lowerText.contains("원") && lowerText.contains("할인")) {
            return brandName + " 생일 할인쿠폰";
        }
        if (lowerText.contains("생일") && (lowerText.contains("쿠폰") || lowerText.contains("coupon"))) {
            return brandName + " 생일축하쿠폰";
        }
        if (lowerText.contains("생일") && (lowerText.contains("할인") || lowerText.contains("discount"))) {
            return brandName + " 생일 할인 혜택";
        }
        return brandName + " 생일 혜택 후보";
    }

    private String findFreeItem(List<String> benefitDetails) {
        for (String detail : benefitDetails) {
            String lowerDetail = detail.toLowerCase(Locale.ROOT);
            if (lowerDetail.contains("무료")) {
                String normalized = detail.replaceAll(".*?([가-힣A-Za-z0-9 ]{2,30})\\s*(1개\\s*)?무료.*", "$1").trim();
                if (StringUtils.hasText(normalized) && normalized.length() <= 30) {
                    return normalized;
                }
                return "상품";
            }
        }
        return null;
    }

    private String buildSummary(String brandName, List<String> benefitDetails, String evidenceText, String lowerEvidenceText) {
        if (!benefitDetails.isEmpty()) {
            String detailSummary = String.join(", ", benefitDetails.stream().limit(5).toList());
            if (benefitDetails.stream().anyMatch(detail -> detail.contains("CGV") || detail.contains("VIPS") || detail.contains("계절밥상") || detail.contains("더플레이스"))) {
                return truncate(brandName + " 회원에게 매년 1회 " + detailSummary + " 등 제휴 브랜드 생일축하 쿠폰을 제공하는 멤버십 혜택입니다.", MAX_SUMMARY_LENGTH);
            }
            return truncate(brandName + " 회원에게 매년 1회 여러 제휴 브랜드에서 사용할 수 있는 생일축하 쿠폰을 제공하는 멤버십 혜택입니다. 대표 혜택: " + detailSummary, MAX_SUMMARY_LENGTH);
        }
        List<String> sentences = splitSentences(evidenceText);
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase(Locale.ROOT);
            if (containsBirthdayKeyword(lowerSentence) && BENEFIT_KEYWORDS.stream().anyMatch(lowerSentence::contains)) {
                return truncate(sentence, MAX_SUMMARY_LENGTH);
            }
        }
        if (lowerEvidenceText.contains("쿠폰") || lowerEvidenceText.contains("coupon")) {
            return truncate(brandName + "의 생일 쿠폰/혜택 후보입니다. 공식 출처의 근거 문장을 확인하세요.", MAX_SUMMARY_LENGTH);
        }
        return truncate(brandName + "의 생일 혜택 후보입니다. 공식 출처의 근거 문장을 확인하세요.", MAX_SUMMARY_LENGTH);
    }

    private String joinLines(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return String.join("\n", values);
    }

    private String cleanCouponPrefix(String text) {
        return text == null ? "" : text.replaceAll("^\\[[^]]*]\\s*", "").trim();
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
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
