package com.noh.zup.domain;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noh.zup.domain.benefit.VerificationStatus;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.tag.TagRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:zup-admin-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void createCategorySucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Test Category",
                                "slug", "test-category",
                                "displayOrder", 99,
                                "isActive", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("category created"))
                .andExpect(jsonPath("$.data.slug").value("test-category"));
    }

    @Test
    void createTagSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Test Tag",
                                "slug", "test-tag",
                                "displayOrder", 99,
                                "isActive", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("tag created"))
                .andExpect(jsonPath("$.data.slug").value("test-tag"));
    }

    @Test
    void createBrandSucceeds() throws Exception {
        Long categoryId = categoryRepository.findBySlug("cafe").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "categoryId", categoryId,
                                "name", "Test Brand",
                                "slug", "test-brand",
                                "description", "Test brand description",
                                "isActive", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("brand created"))
                .andExpect(jsonPath("$.data.slug").value("test-brand"));
    }

    @Test
    void duplicateSlugReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Duplicate Cafe",
                                "slug", "cafe",
                                "displayOrder", 100,
                                "isActive", true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category slug already exists"));
    }

    @Test
    void createBenefitAndPublishThenPublicBrandDetailShowsBenefit() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Published test benefit");

        mockMvc.perform(patch("/api/v1/admin/benefits/{id}/status", benefitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("verificationStatus", "PUBLISHED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/brands/starbucks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefits", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void statusChangeCreatesVerificationLog() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Verification log test benefit");

        mockMvc.perform(patch("/api/v1/admin/benefits/{id}/status", benefitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "verificationStatus", "PUBLISHED",
                                "memo", "Official page checked"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("verification logs fetched"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].benefitId").value(benefitId))
                .andExpect(jsonPath("$.data[0].beforeStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].afterStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data[0].memo").value("Official page checked"));
    }

    @Test
    void sameStatusChangeDoesNotCreateVerificationLog() throws Exception {
        Long brandId = brandRepository.findBySlug("outback").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Same status test benefit");

        mockMvc.perform(patch("/api/v1/admin/benefits/{id}/status", benefitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "verificationStatus", "DRAFT",
                                "memo", "No status transition"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("DRAFT"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void recentVerificationLogsCanBeFetched() throws Exception {
        Long brandId = brandRepository.findBySlug("twosome-place").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Recent log test benefit");

        mockMvc.perform(patch("/api/v1/admin/benefits/{id}/status", benefitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "verificationStatus", "NEEDS_CHECK",
                                "memo", "Needs re-check"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/verification-logs/recent?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("recent verification logs fetched"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].benefitId").value(benefitId))
                .andExpect(jsonPath("$.data[0].beforeStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].afterStatus").value("NEEDS_CHECK"));
    }

    @Test
    void missingBenefitVerificationLogsReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Benefit not found"));
    }

    @Test
    void addBenefitSourceSucceeds() throws Exception {
        Long brandId = brandRepository.findBySlug("oliveyoung").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Source test benefit");

        mockMvc.perform(post("/api/v1/admin/benefits/{benefitId}/sources", benefitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceType", "OFFICIAL_HOME",
                                "sourceUrl", "https://example.com",
                                "sourceTitle", "Official page"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("benefit source created"))
                .andExpect(jsonPath("$.data.sourceType").value("OFFICIAL_HOME"));
    }

    @Test
    void addBenefitTagSucceeds() throws Exception {
        Long brandId = brandRepository.findBySlug("cgv").orElseThrow().getId();
        Long tagId = tagRepository.findBySlug("free").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Tag test benefit");

        mockMvc.perform(post("/api/v1/admin/benefits/{benefitId}/tags", benefitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tagId", tagId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("benefit tag added"))
                .andExpect(jsonPath("$.data.tags", hasSize(1)));
    }

    @Test
    void unknownBrandIdWhenCreatingBenefitReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/admin/benefits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", 999999,
                                "title", "Missing brand benefit",
                                "summary", "Missing brand test summary",
                                "benefitType", "COUPON"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Brand not found"));
    }

    @Test
    void createReportSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "reportType", "NEW_BENEFIT",
                                "content", "I found a new official birthday benefit.",
                                "referenceUrl", "https://example.com/source",
                                "email", "user@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("report received"))
                .andExpect(jsonPath("$.data.reportType").value("NEW_BENEFIT"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void createReportWithBrandSucceeds() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", brandId,
                                "reportType", "WRONG_INFO",
                                "content", "The benefit condition seems different now."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandId").value(brandId))
                .andExpect(jsonPath("$.data.brandName").value("스타벅스"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void createReportWithUnknownBrandReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", 999999,
                                "reportType", "WRONG_INFO",
                                "content", "This report references a missing brand."
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Brand not found"));
    }

    @Test
    void wrongInfoReportWithBenefitChangesBenefitToNeedsCheckAndCreatesVerificationLog() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Wrong info report transition benefit", VerificationStatus.PUBLISHED);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", brandId,
                                "benefitId", benefitId,
                                "reportType", "WRONG_INFO",
                                "content", "The official app condition has changed.",
                                "referenceUrl", "https://example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefitId").value(benefitId))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));

        mockMvc.perform(get("/api/v1/admin/benefits/{id}", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("NEEDS_CHECK"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].beforeStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data[0].afterStatus").value("NEEDS_CHECK"))
                .andExpect(jsonPath("$.data[0].memo")
                        .value("\uC0AC\uC6A9\uC790 \uC81C\uBCF4\uB85C \uAC80\uC218 \uD544\uC694 \uC804\uD658: WRONG_INFO"));

        mockMvc.perform(get("/api/v1/admin/review-needed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + benefitId + ")]", hasSize(1)));
    }

    @Test
    void reportOnAlreadyNeedsCheckBenefitDoesNotCreateDuplicateVerificationLog() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Already needs check report benefit", VerificationStatus.NEEDS_CHECK);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", brandId,
                                "benefitId", benefitId,
                                "reportType", "WRONG_INFO",
                                "content", "This benefit is still wrong."
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void newBenefitReportDoesNotChangeExistingBenefitStatus() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "New benefit report ignored existing benefit", VerificationStatus.PUBLISHED);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", brandId,
                                "benefitId", benefitId,
                                "reportType", "NEW_BENEFIT",
                                "content", "I found another birthday benefit."
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/benefits/{id}", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void etcReportDoesNotChangeBenefitStatus() throws Exception {
        Long brandId = brandRepository.findBySlug("starbucks").orElseThrow().getId();
        Long benefitId = createBenefit(brandId, "Etc report ignored benefit", VerificationStatus.PUBLISHED);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "brandId", brandId,
                                "benefitId", benefitId,
                                "reportType", "ETC",
                                "content", "This is a general report."
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/benefits/{id}", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/admin/benefits/{benefitId}/verification-logs", benefitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void adminReportsCanBeFetchedAndResolved() throws Exception {
        Long reportId = createReport("Benefit ended report", "BENEFIT_ENDED");

        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("reports fetched"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(patch("/api/v1/admin/reports/{id}/status", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "status", "RESOLVED",
                                "adminMemo", "Official page checked and reflected"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("report status updated"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.adminMemo").value("Official page checked and reflected"))
                .andExpect(jsonPath("$.data.resolvedAt").exists());

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolvedReportCount", greaterThanOrEqualTo(1)));
    }

    private Long createBenefit(Long brandId, String title) throws Exception {
        return createBenefit(brandId, title, null);
    }

    private Long createBenefit(Long brandId, String title, VerificationStatus verificationStatus) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("brandId", brandId);
        request.put("title", title);
        request.put("summary", "Official source verification test summary");
        request.put("benefitType", "COUPON");
        request.put("birthdayTimingType", "BIRTHDAY_MONTH");
        request.put("isActive", true);
        if (verificationStatus != null) {
            request.put("verificationStatus", verificationStatus.name());
        }

        MvcResult result = mockMvc.perform(post("/api/v1/admin/benefits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("benefit created"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("id").asLong();
    }

    private Long createReport(String content, String reportType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "reportType", reportType,
                                "content", content
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("data").get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
