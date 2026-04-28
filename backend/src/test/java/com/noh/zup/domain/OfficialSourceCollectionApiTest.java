package com.noh.zup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.collection.CollectionTriggerType;
import com.noh.zup.domain.collection.SourceWatchService;
import com.noh.zup.domain.collection.fetch.FetchResult;
import com.noh.zup.domain.collection.fetch.OfficialSourceFetcher;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest
@WithMockUser(roles = "SUPER_ADMIN")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:zup-collection-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OfficialSourceCollectionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SourceWatchService sourceWatchService;

    @MockBean
    private OfficialSourceFetcher officialSourceFetcher;

    @Test
    void sourceWatchCanBeCreatedAndUpdated() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Original title");

        mockMvc.perform(patch("/api/v1/admin/source-watches/{id}", sourceWatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Updated title",
                                "sourceType", "OFFICIAL_FAQ"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("source watch updated"))
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.sourceType").value("OFFICIAL_FAQ"));

        mockMvc.perform(patch("/api/v1/admin/source-watches/{id}/active", sourceWatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void collectCreatesSnapshotAndBirthdayCouponCandidate() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Birthday benefit page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetched").value(true))
                .andExpect(jsonPath("$.data.sameAsPrevious").value(false))
                .andExpect(jsonPath("$.data.candidateCount").value(1));

        long candidateId = getCandidateIdBySourceWatch(sourceWatchId);
        mockMvc.perform(get("/api/v1/admin/benefit-candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceWatchId").value(sourceWatchId))
                .andExpect(jsonPath("$.data.status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.occasionType").value("BIRTHDAY"));

        mockMvc.perform(get("/api/v1/admin/source-watches/{id}/collection-runs", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("source watch collection runs fetched"))
                .andExpect(jsonPath("$.data[0].triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].fetched").value(true))
                .andExpect(jsonPath("$.data[0].sameAsPrevious").value(false))
                .andExpect(jsonPath("$.data[0].candidateCount").value(1))
                .andExpect(jsonPath("$.data[0].durationMillis").exists());
    }

    @Test
    void collectCleansNavigationTextFromCandidateEvidenceAndSummary() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, noisyBirthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("cgv").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Noisy birthday coupon page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateCount").value(1));

        long candidateId = getCandidateIdBySourceWatch(sourceWatchId);
        MvcResult result = mockMvc.perform(get("/api/v1/admin/benefit-candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("CGV 생일축하 10종 쿠폰"))
                .andReturn();

        JsonNode candidate = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
        String summary = candidate.get("summary").asText();
        String evidenceText = candidate.get("evidenceText").asText();

        assertThat(summary).contains("생일");
        assertThat(summary).doesNotContain("개인정보처리방침", "전체메뉴", "고객센터");
        assertThat(evidenceText).contains("생일 축하");
        assertThat(evidenceText).doesNotContain("개인정보처리방침", "전체메뉴", "고객센터", "SNS");
        assertThat(evidenceText.length()).isLessThanOrEqualTo(500);
    }

    @Test
    void collectExtractsBirthdayCouponDetailsAndUsageGuide() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, cjOneBirthdayCouponHtml()));
        Long brandId = ensureBrand("CJ ONE", "cj-one", "movie-culture");
        Long sourceWatchId = createSourceWatch(brandId, "CJ ONE birthday coupon page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateCount").value(1));

        long candidateId = getCandidateIdBySourceWatch(sourceWatchId);
        MvcResult result = mockMvc.perform(get("/api/v1/admin/benefit-candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("CJ ONE 생일축하 10종 쿠폰"))
                .andExpect(jsonPath("$.data.benefitDetailText").exists())
                .andExpect(jsonPath("$.data.usageGuideText").exists())
                .andReturn();

        JsonNode candidate = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
        String benefitDetailText = candidate.get("benefitDetailText").asText();
        String usageGuideText = candidate.get("usageGuideText").asText();
        String summary = candidate.get("summary").asText();

        assertThat(benefitDetailText).contains("50% 할인", "10,000원 할인", "무료");
        assertThat(usageGuideText).contains("회원만 이용", "현금으로 교환", "타인에게 양도", "1년에 1번 지급");
        assertThat(summary).contains("CGV", "VIPS");
        assertThat(usageGuideText.length()).isLessThanOrEqualTo(700);

        MvcResult approveResult = mockMvc.perform(post("/api/v1/admin/benefit-candidates/{id}/approve", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isOk())
                .andReturn();
        long benefitId = objectMapper.readTree(approveResult.getResponse().getContentAsByteArray())
                .get("data")
                .get("benefitId")
                .asLong();

        mockMvc.perform(get("/api/v1/admin/benefit-candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(get("/api/v1/admin/benefits/{id}", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionSummary").value(org.hamcrest.Matchers.containsString("회원만 이용")));
    }

    @Test
    void sameContentHashStoresSameSnapshotAndSkipsCandidateCreation() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("outback").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Same hash page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sameAsPrevious").value(false))
                .andExpect(jsonPath("$.data.candidateCount").value(1));

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sameAsPrevious").value(true))
                .andExpect(jsonPath("$.data.candidateCount").value(0));

        mockMvc.perform(get("/api/v1/admin/benefit-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.sourceWatchId == " + sourceWatchId + ")]", hasSize(1)));

        mockMvc.perform(get("/api/v1/admin/source-watches/{id}/collection-runs", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].sameAsPrevious").value(true))
                .andExpect(jsonPath("$.data[0].candidateCount").value(0));
    }

    @Test
    void collectWithoutBirthdayKeywordDoesNotCreateCandidate() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, """
                <html><body><main>회원에게 제공되는 쿠폰과 할인 혜택 안내입니다. 앱에서 확인할 수 있습니다.</main></body></html>
                """));
        Long brandId = brandRepository.findBySlug("cgv").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Coupon page without birthday");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateCount").value(0));
    }

    @Test
    void inactiveSourceWatchCollectCreatesSkippedCollectionRun() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Inactive run page");

        mockMvc.perform(patch("/api/v1/admin/source-watches/{id}/active", sourceWatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", false))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetched").value(false));

        mockMvc.perform(get("/api/v1/admin/source-watches/{id}/collection-runs", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.data[0].failureReason").value("SOURCE_WATCH_INACTIVE"))
                .andExpect(jsonPath("$.data[0].candidateCount").value(0));
    }

    @Test
    void fetchFailureCreatesFailedCollectionRun() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.failure(500, "HTTP status 500"));
        Long brandId = brandRepository.findBySlug("cgv").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Fetch failure page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetched").value(false));

        mockMvc.perform(get("/api/v1/admin/source-watches/{id}/collection-runs", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data[0].failureReason").value("FETCH_FAILED"))
                .andExpect(jsonPath("$.data[0].errorMessage").value("HTTP status 500"));
    }

    @Test
    void scheduledCollectCreatesScheduledCollectionRunAndRecentRunsCanBeFetched() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("outback").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Scheduled run page");

        sourceWatchService.collect(sourceWatchId, CollectionTriggerType.SCHEDULED);

        mockMvc.perform(get("/api/v1/admin/source-watches/{id}/collection-runs", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].triggerType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/admin/collection-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("collection runs fetched"))
                .andExpect(jsonPath("$.data[?(@.sourceWatchId == " + sourceWatchId + ")]", hasSize(1)));
    }

    @Test
    void dashboardIncludesCollectionSummary() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();

        when(officialSourceFetcher.fetch(anyString()))
                .thenReturn(FetchResult.success(200, birthdayCouponHtml()))
                .thenReturn(FetchResult.failure(500, "HTTP status 500"));

        Long successSourceWatchId = createSourceWatch(brandId, "Dashboard success page");
        Long failedSourceWatchId = createSourceWatch(brandId, "Dashboard failed page");
        Long skippedSourceWatchId = createSourceWatch(brandId, "Dashboard skipped page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", successSourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateCount").value(1));
        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", failedSourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetched").value(false));
        mockMvc.perform(patch("/api/v1/admin/source-watches/{id}/active", skippedSourceWatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", false))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", skippedSourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fetched").value(false));

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collectionSummary.totalSourceWatchCount", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.collectionSummary.activeSourceWatchCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.collectionSummary.pendingCandidateCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.collectionSummary.recentSuccessRunCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.collectionSummary.recentFailedRunCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.collectionSummary.recentSkippedRunCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.collectionSummary.recentFailedRuns", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.collectionSummary.recentFailedRuns[0].failureReason").value("FETCH_FAILED"));
    }

    @Test
    void candidateStatusCanBeChangedButDoesNotAppearInPublicBenefitApi() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("oliveyoung").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Review queue page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk());

        long candidateId = getCandidateIdBySourceWatch(sourceWatchId);

        mockMvc.perform(patch("/api/v1/admin/benefit-candidates/{id}/status", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "status", "APPROVED",
                                "reviewMemo", "Official source checked"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewMemo").value("Official source checked"));

        mockMvc.perform(get("/api/v1/benefits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void approveCandidateCreatesBenefitSourceVerificationLogAndKeepsPublicApiHidden() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("twosome-place").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Candidate approval page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateCount").value(1));

        long candidateId = getCandidateIdBySourceWatch(sourceWatchId);
        MvcResult approveResult = mockMvc.perform(post("/api/v1/admin/benefit-candidates/{id}/approve", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Approved birthday coupon",
                                "summary", "Admin reviewed birthday coupon summary",
                                "benefitType", "COUPON",
                                "occasionType", "BIRTHDAY",
                                "birthdayTimingType", "BIRTHDAY_MONTH",
                                "usageCondition", "Use in official app during birthday month",
                                "adminMemo", "Approved from official source"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("benefit candidate approved"))
                .andExpect(jsonPath("$.data.candidateId").value(candidateId))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andReturn();

        JsonNode approveResponse = objectMapper.readTree(approveResult.getResponse().getContentAsString());
        long benefitId = approveResponse.get("data").get("benefitId").asLong();

        mockMvc.perform(get("/api/v1/admin/benefit-candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBenefitId").value(benefitId))
                .andExpect(jsonPath("$.data.approvedAt").exists());

        mockMvc.perform(get("/api/v1/admin/benefits/{id}", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Approved birthday coupon"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/sources", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sourceUrl").value("https://example.com/official-benefit"))
                .andExpect(jsonPath("$.data[0].sourceType").value("OFFICIAL_HOME"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].beforeStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].afterStatus").value("VERIFIED"));

        mockMvc.perform(get("/api/v1/benefits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + benefitId + ")]", empty()));

        mockMvc.perform(post("/api/v1/admin/benefit-candidates/{id}/approve", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Candidate is already approved"));
    }

    @Test
    void rejectedCandidateCannotBeApproved() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Rejected candidate page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk());
        long candidateId = getCandidateIdBySourceWatch(sourceWatchId);

        mockMvc.perform(patch("/api/v1/admin/benefit-candidates/{id}/status", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "REJECTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(post("/api/v1/admin/benefit-candidates/{id}/approve", candidateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rejected candidate cannot be approved"));
    }

    private Long createSourceWatch(Long brandId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/source-watches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", brandId,
                                "sourceType", "OFFICIAL_HOME",
                                "title", title,
                                "url", "https://example.com/official-benefit",
                                "isActive", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("source watch created"))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("id").asLong();
    }

    private Long ensureBrand(String name, String slug, String categorySlug) {
        return brandRepository.findBySlug(slug)
                .map(Brand::getId)
                .orElseGet(() -> {
                    var category = categoryRepository.findBySlug(categorySlug).orElseThrow();
                    return brandRepository.save(new Brand(category, name, slug)).getId();
                });
    }

    private long getCandidateIdBySourceWatch(Long sourceWatchId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/benefit-candidates"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode candidates = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode candidate : candidates) {
            if (candidate.get("sourceWatchId").asLong() == sourceWatchId) {
                return candidate.get("id").asLong();
            }
        }
        throw new IllegalStateException("Candidate not found for sourceWatchId=" + sourceWatchId);
    }

    private String birthdayCouponHtml() {
        return """
                <html>
                    <head><style>.hidden { display: none; }</style><script>window.alert('x');</script></head>
                    <body>
                        <nav>navigation</nav>
                        <main>회원 생일에는 앱에서 birthday coupon을 받을 수 있습니다. 생일 쿠폰은 무료 gift 혜택으로 제공됩니다.</main>
                        <footer>footer</footer>
                    </body>
                </html>
                """;
    }

    private String noisyBirthdayCouponHtml() {
        return """
                <html>
                    <body>
                        <nav>나의 ONE 나의포인트 고객센터 개인정보처리방침 전체메뉴 닫기</nav>
                        <main>
                            <h1>생일축하쿠폰</h1>
                            <p>CJ ONE 회원이라면 누구나 생일 축하 10종 쿠폰 증정</p>
                            <p>CJ ONE 회원은 매년 1회 생일 축하쿠폰 혜택을 받을 수 있습니다.</p>
                            <p>설정된 생일 정보를 확인하세요!</p>
                        </main>
                        <footer>개인정보처리방침 고객센터 SNS</footer>
                    </body>
                </html>
                """;
    }

    private String cjOneBirthdayCouponHtml() {
        return """
                <html>
                    <body>
                        <main>
                            <h1>생일 축하 10종 쿠폰 증정</h1>
                            <section>
                                <p>CGV 매점 콤보 구매 시 50% 할인</p>
                                <p>VIPS 10,000원 할인</p>
                                <p>계절밥상 3,000원 할인</p>
                                <p>더플레이스 리코타 프루타 샐러드 1개 무료</p>
                                <p>CJ THE MARKET 3,000원 중복 할인</p>
                            </section>
                            <section>
                                <h2>이용안내</h2>
                                <p>CJ ONE 회원만 이용 가능합니다.</p>
                                <p>CJ ONE 쿠폰은 현금으로 교환 및 타인에게 양도할 수 없습니다.</p>
                                <p>생일축하쿠폰은 1년에 1번 지급되며, 한번 발급되면 추가 발급되지 않습니다.</p>
                                <p>CJ ONE 회원정보에 등록된 생년월일을 기준으로 쿠폰이 발급됩니다.</p>
                            </section>
                        </main>
                    </body>
                </html>
                """;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
