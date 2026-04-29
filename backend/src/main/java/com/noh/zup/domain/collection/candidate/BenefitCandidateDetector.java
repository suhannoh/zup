package com.noh.zup.domain.collection.candidate;

import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import com.noh.zup.domain.collection.BenefitCandidate;
import com.noh.zup.domain.collection.BenefitCandidateRepository;
import com.noh.zup.domain.collection.PageSnapshot;
import com.noh.zup.domain.collection.SourceWatch;
import com.noh.zup.domain.collection.extract.HtmlTextExtractor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BenefitCandidateDetector {

    private static final List<String> BENEFIT_KEYWORDS = List.of(
            "쿠폰", "혜택", "할인", "무료", "증정", "포인트", "적립", "coupon", "benefit", "discount", "free", "gift"
    );
    private static final List<String> RELATED_KEYWORDS = List.of(
            "생일", "birthday", "쿠폰", "coupon", "혜택", "benefit", "할인", "무료", "증정", "회원", "멤버십"
    );
    private static final List<String> APP_MEMBERSHIP_KEYWORDS = List.of("앱", "회원", "멤버십", "membership");
    private static final List<String> MENU_NOISE_KEYWORDS = List.of(
            "개인정보처리방침", "이용약관", "고객센터", "전체메뉴", "닫기", "로그인", "매장 찾기",
            "네이버블로그", "인스타그램", "유튜브", "facebook", "instagram", "youtube", "footer", "navigation"
    );
    private static final List<String> DETAIL_KEYWORDS = List.of(
            "할인", "무료", "증정", "쿠폰", "포인트", "적립", "원", "%", "퍼센트", "1+1", "콤보", "세트",
            "아메리카노", "음료", "케이크", "디저트", "입장권", "관람권", "샐러드", "프렌치프라이",
            "나초칩", "타임캡슐", "자물쇠", "free", "discount", "coupon", "point", "gift", "combo"
    );
    private static final List<String> USAGE_GUIDE_KEYWORDS = List.of(
            "이용안내", "이용 가능", "회원만", "현금", "교환", "양도", "발급", "지급", "1년에 1번",
            "추가 발급", "생년월일", "생일 정보", "회원정보", "사용 조건", "최소 구매", "유효기간", "변경", "제외"
    );
    private static final List<String> DETAIL_REJECT_KEYWORDS = List.of(
            "선택됨", "자세히 보기", "확인", "안내", "상세조건", "회원정보", "설정된 생일 정보를 확인하세요",
            "발급된 쿠폰은", "제휴사 사정에 의해", "현금으로 교환", "타인에게 양도",
            "가입축하쿠폰", "가입 축하", "최초 가입", "제휴가입", "아이디 등록", "회원 전환",
            "잔액 0원", "더보기", "이미지 없음"
    );
    private static final List<String> BIRTHDAY_CONTEXT_KEYWORDS = List.of(
            "생일", "생일 축하", "birthday", "birth", "생년월일", "무료 음료 e-쿠폰", "생일 쿠폰"
    );
    private static final List<String> REWARD_EXCLUSION_KEYWORDS = List.of(
            "별 적립", "별 15개", "별 50개", "푸드 바우처", "md 바우처", "등급 전용",
            "gold", "welcome", "green", "쿠폰 교환", "포인트 적립", "이벤트"
    );
    private static final List<String> UNRELATED_USAGE_KEYWORDS = List.of(
            "가입축하쿠폰", "가입 축하", "최초 가입", "제휴가입", "아이디 등록", "회원 전환"
    );
    private static final Pattern MONEY_OR_PERCENT = Pattern.compile("([0-9][0-9,]*\\s*원|[0-9]+\\s*%|[0-9]+\\s*퍼센트)");
    private static final Pattern CONDITION_ONLY = Pattern.compile("^[0-9][0-9,]*\\s*(?:만원|원)?\\s*이상\\s*(구매|주문|결제)\\s*시\\.?$");
    private static final Pattern UI_MONEY_ONLY = Pattern.compile("^\\(?[0-9][0-9,]*\\s*원\\)?$");
    private static final Pattern UI_RESIDUAL = Pattern.compile("^(잔액\\s*0원|자세히 보기|확인|닫기|더보기|이용안내|상세조건|쿠폰으로|제공\\s*\\(|이미지 없음)$");
    private static final Pattern CONDITION_PREFIX = Pattern.compile("^(.{0,80}?(?:[0-9][0-9,]*\\s*(?:만원|원)?\\s*이상\\s*(?:구매|주문|결제)\\s*시|콤보\\s*구매\\s*시|상품\\s*구매\\s*시))[,\\s]*(.+)$");
    private static final Pattern STANDALONE_DISCOUNT = Pattern.compile("^([0-9][0-9,]*\\s*(?:만원|원)?|[0-9]+\\s*%|[0-9]+\\s*퍼센트)\\s*(?:중복\\s*)?할인\\.?$");
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?。])\\s+|(?<=다\\.)\\s*|\\n+");
    private static final int MAX_SENTENCE_LENGTH = 300;
    private static final int MAX_EVIDENCE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final int MAX_BENEFIT_DETAIL_COUNT = 7;
    private static final int MAX_USAGE_GUIDE_LENGTH = 700;

    private final BenefitCandidateRepository benefitCandidateRepository;

    public BenefitCandidateDetector(BenefitCandidateRepository benefitCandidateRepository) {
        this.benefitCandidateRepository = benefitCandidateRepository;
    }

    public List<BenefitCandidate> detect(SourceWatch sourceWatch, PageSnapshot snapshot) {
        return detect(sourceWatch, snapshot, null);
    }

    public List<BenefitCandidate> detect(SourceWatch sourceWatch, PageSnapshot snapshot, Long collectionRunId) {
        List<BenefitCandidate> createdCandidates = new ArrayList<>();
        detect(sourceWatch, snapshot, collectionRunId, createdCandidates);
        return createdCandidates;
    }

    public BenefitCandidateDetectionResult detectWithResult(SourceWatch sourceWatch, PageSnapshot snapshot) {
        return detectWithResult(sourceWatch, snapshot, null);
    }

    public BenefitCandidateDetectionResult detectWithResult(
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            Long collectionRunId
    ) {
        return detect(sourceWatch, snapshot, collectionRunId, new ArrayList<>());
    }

    private BenefitCandidateDetectionResult detect(
            SourceWatch sourceWatch,
            PageSnapshot snapshot,
            Long collectionRunId,
            List<BenefitCandidate> createdCandidates
    ) {
        String text = snapshot.getExtractedText();
        if (!StringUtils.hasText(text)) {
            return new BenefitCandidateDetectionResult(0, 0);
        }

        BlockAnalysis analysis = analyzeBlocks(text);
        String candidateText = analysis.candidateText();
        String lowerText = candidateText.toLowerCase(Locale.ROOT);
        if (!containsBirthdayKeyword(lowerText) || BENEFIT_KEYWORDS.stream().noneMatch(lowerText::contains)) {
            return new BenefitCandidateDetectionResult(0, 0);
        }

        String evidenceText = extractEvidenceText(candidateText);
        if (!StringUtils.hasText(evidenceText)) {
            return new BenefitCandidateDetectionResult(0, 0);
        }

        List<String> sentences = splitSentences(candidateText);
        DetailExtraction detailExtraction = extractBenefitDetailsWithExclusions(sentences);
        List<String> benefitDetails = detailExtraction.details();
        String benefitDetailText = joinLines(benefitDetails);
        String usageGuideText = extractUsageGuideText(sentences);
        String detailAndEvidenceText = (benefitDetailText == null ? "" : benefitDetailText + " ") + evidenceText;
        String lowerDetailAndEvidenceText = detailAndEvidenceText.toLowerCase(Locale.ROOT);
        List<String> warnings = buildWarnings(analysis, detailExtraction, benefitDetails, lowerText, snapshot.getBenefitDetailImageSources());

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
                collectionRunId,
                buildTitle(sourceWatch.getBrand().getName(), lowerDetailAndEvidenceText),
                buildSummary(sourceWatch.getBrand().getName(), benefitDetails, evidenceText),
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
                joinLines(warnings),
                analysis.contextEvidence(),
                joinLines(mergeExcludedTexts(detailExtraction.excludedTexts(), analysis.excludedBlocks())),
                calculateConfidence(sourceWatch, lowerText, warnings)
        );
        if (!warnings.isEmpty()) {
            candidate.requireManualReview("규칙 기반 추출 경고가 있어 수동 검수가 필요합니다.");
        }
        createdCandidates.add(benefitCandidateRepository.save(candidate));
        return new BenefitCandidateDetectionResult(1, 0);
    }

    private BlockAnalysis analyzeBlocks(String text) {
        String[] rawBlocks = text.contains(HtmlTextExtractor.BLOCK_SEPARATOR)
                ? text.split(Pattern.quote(HtmlTextExtractor.BLOCK_SEPARATOR))
                : new String[] { text };
        List<String> birthdayBlocks = new ArrayList<>();
        List<String> conditionBlocks = new ArrayList<>();
        List<String> excludedBlocks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String rawBlock : rawBlocks) {
            String block = rawBlock.replaceAll("\\s+", " ").trim();
            if (!StringUtils.hasText(block)) {
                continue;
            }
            BlockContext context = classifyBlock(block);
            if (context == BlockContext.FOOTER_OR_NAV || context == BlockContext.FAQ) {
                excludedBlocks.add(truncate(block, 120));
                continue;
            }
            if (context == BlockContext.MEMBERSHIP_REWARD || context == BlockContext.GRADE_BENEFIT) {
                excludedBlocks.add(truncate(block, 120));
                warnings.add("일반 리워드 문맥이 섞여 일부 항목을 제외했습니다.");
                continue;
            }
            if (context == BlockContext.BIRTHDAY_BENEFIT) {
                birthdayBlocks.add(block);
                continue;
            }
            if (context == BlockContext.CONDITION_NOTICE || context == BlockContext.COUPON_USAGE_GUIDE) {
                conditionBlocks.add(block);
            }
        }

        String candidateText = String.join("\n", birthdayBlocks);
        if (StringUtils.hasText(candidateText) && !conditionBlocks.isEmpty()) {
            candidateText = candidateText + "\n" + String.join("\n", conditionBlocks.stream().limit(3).toList());
        }
        if (!StringUtils.hasText(candidateText)) {
            candidateText = text;
            warnings.add("생일 문맥이 약해 관리자 검수가 필요합니다.");
        }
        return new BlockAnalysis(
                candidateText,
                truncate(String.join(" ", birthdayBlocks.stream().limit(2).toList()), 300),
                warnings.stream().distinct().toList(),
                excludedBlocks.stream().distinct().toList()
        );
    }

    private BlockContext classifyBlock(String block) {
        String lower = block.toLowerCase(Locale.ROOT);
        if (MENU_NOISE_KEYWORDS.stream().filter(lower::contains).count() >= 2
                || lower.contains("회사소개") || lower.contains("개인정보") || lower.contains("이용약관")) {
            return BlockContext.FOOTER_OR_NAV;
        }
        if (lower.contains("잔액 0원") || lower.contains("선택됨") || lower.contains("자세히 보기")) {
            return BlockContext.FOOTER_OR_NAV;
        }
        if (lower.contains("faq") || lower.contains("자주 묻는")) {
            return BlockContext.FAQ;
        }
        if (BIRTHDAY_CONTEXT_KEYWORDS.stream().anyMatch(lower::contains)) {
            return BlockContext.BIRTHDAY_BENEFIT;
        }
        if (lower.contains("유의사항") || lower.contains("사용 조건") || lower.contains("제외")
                || lower.contains("중복") || lower.contains("발급일") || lower.contains("유효기간")) {
            return BlockContext.CONDITION_NOTICE;
        }
        if (lower.contains("이용안내") || lower.contains("쿠폰 사용") || lower.contains("앱에서")) {
            return BlockContext.COUPON_USAGE_GUIDE;
        }
        if (lower.contains("gold") || lower.contains("green") || lower.contains("welcome") || lower.contains("등급")) {
            return BlockContext.GRADE_BENEFIT;
        }
        if (REWARD_EXCLUSION_KEYWORDS.stream().anyMatch(lower::contains)
                || lower.contains("리워드") || lower.contains("바우처") || lower.contains("포인트")) {
            return BlockContext.MEMBERSHIP_REWARD;
        }
        return BlockContext.UNKNOWN;
    }

    private List<String> extractBenefitDetails(List<String> sentences) {
        return extractBenefitDetailsWithExclusions(sentences).details();
    }

    private DetailExtraction extractBenefitDetailsWithExclusions(List<String> sentences) {
        Set<String> details = new LinkedHashSet<>();
        Set<String> excludedTexts = new LinkedHashSet<>();
        sentences.stream()
                .map(sentence -> new DetailSentence(sentence, normalizeBenefitDetail(sentence)))
                .filter(item -> StringUtils.hasText(item.normalized()))
                .filter(item -> isNotMenuNoise(item.normalized()))
                .forEach(item -> {
                    if (!isBenefitDetailSentence(item.normalized())) {
                        excludedTexts.add(truncate(item.normalized(), 120));
                        return;
                    }
                    if (details.size() < MAX_BENEFIT_DETAIL_COUNT) {
                        details.add(item.normalized());
                    }
                });
        List<String> sortedDetails = details.stream()
                .sorted((left, right) -> Integer.compare(benefitPriority(right), benefitPriority(left)))
                .limit(MAX_BENEFIT_DETAIL_COUNT)
                .toList();
        return new DetailExtraction(new ArrayList<>(sortedDetails), new ArrayList<>(excludedTexts));
    }

    private String normalizeBenefitDetail(String sentence) {
        if (!StringUtils.hasText(sentence)) {
            return null;
        }
        String cleaned = cleanCouponPrefix(sentence)
                .replace("무료% 할인", "무료")
                .replace("무료 % 할인", "무료")
                .replaceAll("\\s+", " ")
                .trim();
        Matcher conditionPrefix = CONDITION_PREFIX.matcher(cleaned);
        if (conditionPrefix.matches()) {
            String conditionPart = cleanCouponPrefix(conditionPrefix.group(1)).trim();
            conditionPart = cleanConditionContext(conditionPart);
            String benefitPart = conditionPrefix.group(2).trim();
            if (StringUtils.hasText(benefitPart)) {
                cleaned = STANDALONE_DISCOUNT.matcher(benefitPart).matches()
                        ? benefitPart + " (조건: " + conditionPart + ")"
                        : conditionPart + " " + benefitPart;
            }
        }
        cleaned = cleaned.replaceAll("^CJ ONE\\s*쿠폰으로\\s*", "").trim();
        if (cleaned.endsWith("선택됨") || UI_RESIDUAL.matcher(cleaned).matches() || UI_MONEY_ONLY.matcher(cleaned).matches()) {
            return null;
        }
        if (cleaned.length() > MAX_SENTENCE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_SENTENCE_LENGTH);
        }
        return cleaned;
    }

    private boolean isBenefitDetailSentence(String sentence) {
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        if (UI_RESIDUAL.matcher(sentence).matches() || UI_MONEY_ONLY.matcher(sentence).matches()) {
            return false;
        }
        if (REWARD_EXCLUSION_KEYWORDS.stream().anyMatch(lowerSentence::contains)) {
            return false;
        }
        if (CONDITION_ONLY.matcher(sentence).matches()) {
            return false;
        }
        if (STANDALONE_DISCOUNT.matcher(sentence).matches()) {
            return false;
        }
        if (lowerSentence.matches(".*쿠폰으로\\.?$")) {
            return false;
        }
        if (DETAIL_REJECT_KEYWORDS.stream().anyMatch(lowerSentence::contains)) {
            return false;
        }
        boolean hasBenefitKeyword = DETAIL_KEYWORDS.stream().anyMatch(lowerSentence::contains);
        if (!hasBenefitKeyword) {
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
        return !guideOnly;
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
            priority += 2;
        }
        if (CONDITION_ONLY.matcher(sentence).matches() || lowerSentence.endsWith("쿠폰으로")) {
            priority -= 20;
        }
        return priority;
    }

    private String extractUsageGuideText(List<String> sentences) {
        List<String> guides = new ArrayList<>();
        for (String sentence : sentences) {
            String normalized = sentence.replaceAll("\\s+", " ").trim();
            String lowerSentence = normalized.toLowerCase(Locale.ROOT);
            if (UNRELATED_USAGE_KEYWORDS.stream().anyMatch(lowerSentence::contains)) {
                continue;
            }
            if (USAGE_GUIDE_KEYWORDS.stream().anyMatch(lowerSentence::contains) && isUsefulGuideSentence(normalized)) {
                guides.add(normalized);
            }
        }
        if (guides.isEmpty()) {
            return null;
        }
        return truncate(String.join(" ", guides.stream().distinct().toList()), MAX_USAGE_GUIDE_LENGTH);
    }

    private String extractEvidenceText(String text) {
        List<String> sentences = splitSentences(text);
        List<String> evidenceSentences = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i).trim();
            String lowerSentence = sentence.toLowerCase(Locale.ROOT);
            if (containsBirthdayKeyword(lowerSentence)) {
                int start = Math.max(0, i - 1);
                int end = Math.min(sentences.size() - 1, i + 1);
                for (int j = start; j <= end && evidenceSentences.size() < 3; j++) {
                    String candidate = sentences.get(j).trim();
                    if (isUsefulEvidenceSentence(candidate)) {
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
            return truncate(text.substring(start, end).trim(), MAX_EVIDENCE_LENGTH);
        }
        return truncate(String.join(" ", evidenceSentences), MAX_EVIDENCE_LENGTH);
    }

    private List<String> splitSentences(String text) {
        String normalized = text
                .replace("원 할인", "원 할인\n")
                .replace("% 할인", "% 할인\n")
                .replaceAll("(?<=다)\\s+", "다.\n");
        String[] rawSentences = SENTENCE_BOUNDARY.split(normalized);
        List<String> sentences = new ArrayList<>();
        for (String rawSentence : rawSentences) {
            String sentence = rawSentence.replaceAll("\\s+", " ").trim();
            if (sentence.length() < 4) {
                continue;
            }
            sentences.add(truncate(sentence, MAX_SENTENCE_LENGTH));
        }
        return mergeConditionAndBenefitSentences(sentences);
    }

    private List<String> mergeConditionAndBenefitSentences(List<String> sentences) {
        List<String> merged = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String current = sentences.get(i);
            if (i + 1 < sentences.size()) {
                String next = sentences.get(i + 1);
                if (isConditionContext(current) && STANDALONE_DISCOUNT.matcher(next).matches()) {
                    merged.add(truncate(current + " " + next, MAX_SENTENCE_LENGTH));
                    i++;
                    continue;
                }
            }
            merged.add(current);
        }
        return merged;
    }

    private boolean isConditionContext(String sentence) {
        String normalized = cleanCouponPrefix(sentence);
        return CONDITION_ONLY.matcher(normalized).matches()
                || normalized.matches(".*(콤보|세트|상품|주문|구매|결제).*시\\.?$")
                || normalized.matches(".*[0-9][0-9,]*\\s*(?:만원|원)?\\s*이상.*");
    }

    private String cleanConditionContext(String conditionPart) {
        if (conditionPart == null) {
            return "";
        }
        return conditionPart
                .replaceAll("(?i)^.*?birthday\\s*", "")
                .replaceAll("^.*?생일\\s*(축하)?\\s*(쿠폰)?\\s*", "")
                .replaceAll("^\\[[^]]*]\\s*", "")
                .trim();
    }

    private boolean isUsefulEvidenceSentence(String sentence) {
        if (!StringUtils.hasText(sentence)) {
            return false;
        }
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        return RELATED_KEYWORDS.stream().anyMatch(lowerSentence::contains) && isNotMenuNoise(sentence);
    }

    private boolean isUsefulGuideSentence(String sentence) {
        return StringUtils.hasText(sentence) && isNotMenuNoise(sentence);
    }

    private boolean isNotMenuNoise(String sentence) {
        String lowerSentence = sentence.toLowerCase(Locale.ROOT);
        long menuKeywordCount = MENU_NOISE_KEYWORDS.stream()
                .filter(lowerSentence::contains)
                .count();
        return menuKeywordCount < 2;
    }

    private BenefitType detectBenefitType(String lowerText) {
        if (lowerText.contains("쿠폰") || lowerText.contains("coupon")) {
            return BenefitType.COUPON;
        }
        if (lowerText.contains("할인") || lowerText.contains("discount")) {
            return BenefitType.DISCOUNT;
        }
        if (lowerText.contains("무료") || lowerText.contains("free")) {
            return BenefitType.FREE_ITEM;
        }
        if (lowerText.contains("증정") || lowerText.contains("gift")) {
            return BenefitType.GIFT;
        }
        return BenefitType.ETC;
    }

    private String buildTitle(String brandName, String lowerText) {
        if (lowerText.contains("10종") && (lowerText.contains("쿠폰") || lowerText.contains("coupon"))) {
            return brandName + " 생일축하 10종 쿠폰";
        }
        if (lowerText.contains("50%") || lowerText.contains("50 %")) {
            return brandName + " 생일 50% 할인";
        }
        if (lowerText.contains("원") && lowerText.contains("할인")) {
            return brandName + " 생일 할인쿠폰";
        }
        if (lowerText.contains("생일") && (lowerText.contains("쿠폰") || lowerText.contains("coupon"))) {
            return brandName + " 생일축하쿠폰";
        }
        return brandName + " 생일 혜택 후보";
    }

    private String buildSummary(String brandName, List<String> benefitDetails, String evidenceText) {
        if (!benefitDetails.isEmpty()) {
            String detailSummary = String.join(", ", benefitDetails.stream().limit(5).toList());
            return truncate(brandName + " 회원에게 매년 1회 여러 제휴 브랜드에서 사용할 수 있는 생일축하 쿠폰을 제공하는 멤버십 혜택입니다. 대표 혜택: " + detailSummary, MAX_SUMMARY_LENGTH);
        }
        List<String> sentences = splitSentences(evidenceText);
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase(Locale.ROOT);
            if (containsBirthdayKeyword(lowerSentence) && BENEFIT_KEYWORDS.stream().anyMatch(lowerSentence::contains)) {
                return truncate(sentence, MAX_SUMMARY_LENGTH);
            }
        }
        return truncate(brandName + "의 생일 쿠폰 또는 생일 혜택 안내입니다. 공식 출처의 근거 문장을 확인하세요.", MAX_SUMMARY_LENGTH);
    }

    private boolean containsBirthdayKeyword(String lowerText) {
        return lowerText.contains("생일") || lowerText.contains("birthday");
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

    private String joinLines(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        return String.join("\n", values);
    }

    private String cleanCouponPrefix(String text) {
        return text == null ? "" : text.replaceAll("^\\[[^]]*]\\s*", "").trim();
    }

    private List<String> buildWarnings(
            BlockAnalysis analysis,
            DetailExtraction detailExtraction,
            List<String> benefitDetails,
            String lowerText,
            String benefitDetailImageSources
    ) {
        List<String> warnings = new ArrayList<>(analysis.warnings());
        if (!detailExtraction.excludedTexts().isEmpty()) {
            warnings.add("UI 잔여 텍스트 또는 조건문 단독 문구를 혜택명에서 제외했습니다.");
        }
        if (benefitDetails.isEmpty()) {
            warnings.add("혜택명과 조건을 명확히 분리하지 못해 관리자 검수가 필요합니다.");
        }
        if (REWARD_EXCLUSION_KEYWORDS.stream().anyMatch(lowerText::contains)) {
            warnings.add("일반 리워드 문맥이 감지되어 생일 후보에서 제외했습니다.");
        }
        if (benefitDetailImageSources != null && benefitDetailImageSources.contains("confidence: low")) {
            warnings.add("브랜드명이 이미지 파일명에만 있거나 불확실해 자동 확정하지 않았습니다.");
        }
        return warnings.stream().distinct().toList();
    }

    private List<String> mergeExcludedTexts(List<String> left, List<String> right) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(left);
        merged.addAll(right);
        return new ArrayList<>(merged);
    }

    private BigDecimal calculateConfidence(SourceWatch sourceWatch, String lowerText, List<String> warnings) {
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
        confidence -= Math.min(0.3, warnings.size() * 0.05);
        return BigDecimal.valueOf(Math.min(confidence, 1.0));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private enum BlockContext {
        BIRTHDAY_BENEFIT,
        MEMBERSHIP_REWARD,
        GRADE_BENEFIT,
        COUPON_USAGE_GUIDE,
        CONDITION_NOTICE,
        FAQ,
        FOOTER_OR_NAV,
        UNKNOWN
    }

    private record BlockAnalysis(
            String candidateText,
            String contextEvidence,
            List<String> warnings,
            List<String> excludedBlocks
    ) {
    }

    private record DetailSentence(String original, String normalized) {
    }

    private record DetailExtraction(List<String> details, List<String> excludedTexts) {
    }
}
