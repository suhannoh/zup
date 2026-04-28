package com.noh.zup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.Category;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.tag.Tag;
import com.noh.zup.domain.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CoreDomainRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void saveAndFindCategoryTagBrand() {
        Category cafe = categoryRepository.save(new Category("카페", "cafe", 1));
        tagRepository.save(new Tag("무료", "free", 1));
        brandRepository.save(new Brand(cafe, "스타벅스", "starbucks"));

        assertThat(categoryRepository.findBySlug("cafe")).isPresent();
        assertThat(tagRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc())
                .extracting(Tag::getSlug)
                .containsExactly("free");
        assertThat(brandRepository.findAllByCategorySlugAndIsActiveTrueOrderByNameAsc("cafe"))
                .extracting(Brand::getSlug)
                .containsExactly("starbucks");
    }
}
