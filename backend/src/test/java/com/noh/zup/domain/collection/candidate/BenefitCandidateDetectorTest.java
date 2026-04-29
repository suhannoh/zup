package com.noh.zup.domain.collection.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.category.Category;
import com.noh.zup.domain.collection.BenefitCandidate;
import com.noh.zup.domain.collection.BenefitCandidateRepository;
import com.noh.zup.domain.collection.PageSnapshot;
import com.noh.zup.domain.collection.SourceWatch;
import com.noh.zup.domain.collection.extract.HtmlTextExtractor;
import com.noh.zup.domain.source.SourceType;
import org.junit.jupiter.api.Test;

class BenefitCandidateDetectorTest {

    private final BenefitCandidateRepository benefitCandidateRepository = mock(BenefitCandidateRepository.class);
    private final BenefitCandidateDetector detector = new BenefitCandidateDetector(benefitCandidateRepository);

    @Test
    void birthdayCandidateExcludesGeneralStarbucksRewardAndUiResidualTexts() {
        SourceWatch sourceWatch = sourceWatch("스타벅스");
        PageSnapshot snapshot = new PageSnapshot(
                sourceWatch,
                "hash",
                String.join(HtmlTextExtractor.BLOCK_SEPARATOR,
                        "생일 무료 음료 e-쿠폰 생일 당월 무료 음료 e-쿠폰 1장 제공 앱에서 쿠폰 선택 후 사용 발급일 포함 31일간 사용 가능",
                        "Gold 별 15개 적립 푸드 바우처 MD 바우처 별 50개 리워드",
                        "잔액 0원 (8,000원) (25,000원) 선택됨 자세히 보기"
                ),
                false
        );
        when(benefitCandidateRepository.existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(anyLong(), anyString(), anyString()))
                .thenReturn(false);
        when(benefitCandidateRepository.save(any(BenefitCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitCandidateDetectionResult result = detector.detectWithResult(sourceWatch, snapshot, 1L);

        assertThat(result.createdCandidateCount()).isEqualTo(1);
        BenefitCandidate candidate = detector.detect(sourceWatch, snapshot, 2L).getFirst();
        assertThat(candidate.getEvidenceText()).hasSizeLessThanOrEqualTo(200);
        assertThat(candidate.getBenefitDetailText()).contains("무료 음료 e-쿠폰");
        assertThat(candidate.getBenefitDetailText()).doesNotContain("별 15개", "별 50개", "잔액 0원", "(8,000원)", "(25,000원)");
        assertThat(candidate.getExtractionWarnings()).contains("일반 리워드 문맥");
        assertThat(candidate.getExcludedTexts()).contains("잔액 0원");
    }

    @Test
    void couponRowConditionsAreSeparatedFromBenefitName() {
        SourceWatch sourceWatch = sourceWatch("CJ ONE");
        PageSnapshot snapshot = new PageSnapshot(
                sourceWatch,
                "hash",
                "생일 축하 쿠폰 [생일축하] 매점 콤보 구매 시 50% 할인"
                        + HtmlTextExtractor.BLOCK_SEPARATOR
                        + "생일 축하 쿠폰 [생일축하] 4만원 이상 주문 시 10,000원 할인",
                null,
                false
        );
        when(benefitCandidateRepository.existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(anyLong(), anyString(), anyString()))
                .thenReturn(false);
        when(benefitCandidateRepository.save(any(BenefitCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitCandidate candidate = detector.detect(sourceWatch, snapshot, 1L).getFirst();

        assertThat(candidate.getBenefitDetailText())
                .contains("50% 할인 (조건: 매점 콤보 구매 시)")
                .contains("10,000원 할인 (조건: 4만원 이상 주문 시)");
        assertThat(candidate.getBenefitDetailText()).doesNotContain("[생일축하]");
    }

    private SourceWatch sourceWatch(String brandName) {
        Brand brand = new Brand(new Category("카테고리", "category", 1), brandName, brandName.toLowerCase());
        SourceWatch sourceWatch = mock(SourceWatch.class);
        when(sourceWatch.getId()).thenReturn(1L);
        when(sourceWatch.getBrand()).thenReturn(brand);
        when(sourceWatch.getSourceType()).thenReturn(SourceType.OFFICIAL_HOME);
        return sourceWatch;
    }
}
