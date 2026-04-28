package com.noh.zup.domain;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.benefit.BenefitDetailItem;
import com.noh.zup.domain.benefit.BenefitDetailItemRepository;
import com.noh.zup.domain.benefit.BenefitRepository;
import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import com.noh.zup.domain.benefit.VerificationStatus;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.source.BenefitSource;
import com.noh.zup.domain.source.BenefitSourceRepository;
import com.noh.zup.domain.source.SourceType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
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

    @Autowired
    private BenefitRepository benefitRepository;

    @Autowired
    private BenefitDetailItemRepository benefitDetailItemRepository;

    @Autowired
    private BenefitSourceRepository benefitSourceRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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

    @Test
    void publicBrandDetailShowsOnlyPublishedActiveBenefitWithDetailsAndSources() throws Exception {
        Brand brand = ensureBrand("CJ ONE", "cj-one-public-test", "movie-culture");
        createBenefit(brand, VerificationStatus.VERIFIED, true, "Verified hidden benefit");
        Benefit published = createBenefit(brand, VerificationStatus.PUBLISHED, true, "CJ ONE 생일축하 10종 쿠폰");
        Benefit inactivePublished = createBenefit(brand, VerificationStatus.PUBLISHED, false, "Inactive hidden benefit");
        benefitSourceRepository.save(source(published));
        benefitSourceRepository.save(source(inactivePublished));
        benefitDetailItemRepository.save(new BenefitDetailItem(
                published,
                null,
                "매점 콤보 구매 시 50% 할인",
                null,
                null,
                "https://example.com/cgv-logo.png",
                1
        ));
        benefitDetailItemRepository.save(new BenefitDetailItem(
                published,
                "VIPS",
                "4만원 이상 주문 시 10,000원 할인",
                null,
                "4만원 이상 주문 시",
                null,
                2
        ));
        BenefitDetailItem inactiveItem = benefitDetailItemRepository.save(new BenefitDetailItem(
                published,
                "숨김",
                "비활성 상세 항목",
                null,
                null,
                null,
                3
        ));
        inactiveItem.changeActive(false);

        mockMvc.perform(get("/api/v1/brands/cj-one-public-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefits", hasSize(1)))
                .andExpect(jsonPath("$.data.benefits[0].title").value("CJ ONE 생일축하 10종 쿠폰"))
                .andExpect(jsonPath("$.data.benefits[0].conditionSummary").value("CJ ONE 회원 대상. 쿠폰별 사용 조건 상이. 현금 교환 및 타인 양도 불가."))
                .andExpect(jsonPath("$.data.benefits[0].usagePeriodDescription").value("생일월 사용 가능"))
                .andExpect(jsonPath("$.data.benefits[0].requiredApp").value(true))
                .andExpect(jsonPath("$.data.benefits[0].requiredMembership").value(true))
                .andExpect(jsonPath("$.data.benefits[0].requiredPurchase").value(true))
                .andExpect(jsonPath("$.data.benefits[0].lastVerifiedAt").value("2026-04-29"))
                .andExpect(jsonPath("$.data.benefits[0].sources[0].sourceTitle").value("CJ ONE 생일축하쿠폰 안내"))
                .andExpect(jsonPath("$.data.benefits[0].sources[0].sourceUrl").value("https://example.com/cj-one-birthday"))
                .andExpect(jsonPath("$.data.benefits[0].detailItems", hasSize(2)))
                .andExpect(jsonPath("$.data.benefits[0].detailItems[0].title").value("매점 콤보 구매 시 50% 할인"))
                .andExpect(jsonPath("$.data.benefits[0].detailItems[0].brandName").doesNotExist())
                .andExpect(jsonPath("$.data.benefits[0].detailItems[1].brandName").value("VIPS"));
    }

    private Brand ensureBrand(String name, String slug, String categorySlug) {
        return brandRepository.findBySlug(slug)
                .orElseGet(() -> brandRepository.save(new Brand(
                        categoryRepository.findBySlug(categorySlug).orElseThrow(),
                        name,
                        slug
                )));
    }

    private Benefit createBenefit(Brand brand, VerificationStatus status, boolean active, String title) {
        Benefit benefit = new Benefit(
                brand,
                title,
                "CJ ONE 회원에게 매년 1회 여러 제휴 브랜드에서 사용할 수 있는 생일축하 쿠폰을 제공하는 멤버십 혜택입니다.",
                BenefitType.COUPON
        );
        benefit.update(
                null,
                null,
                null,
                null,
                null,
                OccasionType.BIRTHDAY,
                BirthdayTimingType.BIRTHDAY_MONTH,
                "CJ ONE 회원 대상. 쿠폰별 사용 조건 상이. 현금 교환 및 타인 양도 불가.",
                true,
                true,
                true,
                null,
                "생일월 사용 가능",
                null,
                null,
                null,
                status,
                LocalDate.of(2026, 4, 29),
                active
        );
        return benefitRepository.save(benefit);
    }

    private BenefitSource source(Benefit benefit) {
        BenefitSource source = new BenefitSource(
                benefit,
                SourceType.OFFICIAL_MEMBERSHIP,
                "https://example.com/cj-one-birthday"
        );
        source.update(null, null, "CJ ONE 생일축하쿠폰 안내", LocalDate.of(2026, 4, 29), null);
        return source;
    }
}
