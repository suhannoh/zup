package com.noh.zup.domain;

import static org.hamcrest.Matchers.empty;
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

        mockMvc.perform(get("/api/v1/admin/benefit-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sourceWatchId").value(sourceWatchId))
                .andExpect(jsonPath("$.data[0].status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data[0].occasionType").value("BIRTHDAY"));
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
    void candidateStatusCanBeChangedButDoesNotAppearInPublicBenefitApi() throws Exception {
        when(officialSourceFetcher.fetch(anyString())).thenReturn(FetchResult.success(200, birthdayCouponHtml()));
        Long brandId = brandRepository.findBySlug("oliveyoung").orElseThrow().getId();
        Long sourceWatchId = createSourceWatch(brandId, "Review queue page");

        mockMvc.perform(post("/api/v1/admin/source-watches/{id}/collect", sourceWatchId))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/admin/benefit-candidates"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        long candidateId = response.get("data").get(0).get("id").asLong();

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
