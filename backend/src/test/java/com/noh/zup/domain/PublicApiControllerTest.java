package com.noh.zup.domain;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:zup-public-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PublicApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCategoriesReturnsSeedCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("categories fetched"))
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].slug").value("cafe"));
    }

    @Test
    void getTagsReturnsSeedTags() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("tags fetched"))
                .andExpect(jsonPath("$.data", hasSize(14)))
                .andExpect(jsonPath("$.data[0].slug").value("free"));
    }

    @Test
    void getBrandsReturnsSeedBrands() throws Exception {
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("brands fetched"))
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[*].id", everyItem(notNullValue())));
    }

    @Test
    void getBrandsWithCategorySlugFiltersBrands() throws Exception {
        mockMvc.perform(get("/api/v1/brands").param("categorySlug", "cafe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].categorySlug", everyItem(is("cafe"))));
    }

    @Test
    void getBrandDetailReturnsBrandAndEmptyBenefits() throws Exception {
        mockMvc.perform(get("/api/v1/brands/starbucks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("brand fetched"))
                .andExpect(jsonPath("$.data.slug").value("starbucks"))
                .andExpect(jsonPath("$.data.categorySlug").value("cafe"))
                .andExpect(jsonPath("$.data.benefits", empty()));
    }

    @Test
    void getUnknownBrandReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/brands/unknown-brand"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Brand not found"));
    }

    @Test
    void getBenefitsReturnsEmptyListWhenNoPublishedBenefitsExist() throws Exception {
        mockMvc.perform(get("/api/v1/benefits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("benefits fetched"))
                .andExpect(jsonPath("$.data", empty()));
    }
}
