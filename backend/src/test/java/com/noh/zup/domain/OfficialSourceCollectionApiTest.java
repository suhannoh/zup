package com.noh.zup.domain;

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
import com.noh.zup.domain.brand.BrandRepository;
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

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
