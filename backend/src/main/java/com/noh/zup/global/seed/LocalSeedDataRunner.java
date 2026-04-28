package com.noh.zup.global.seed;

import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.Category;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.tag.Tag;
import com.noh.zup.domain.tag.TagRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalSeedDataRunner implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final BrandRepository brandRepository;

    public LocalSeedDataRunner(
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            BrandRepository brandRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.brandRepository = brandRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedTags();
        seedBrands();
    }

    private void seedCategories() {
        createCategoryIfAbsent("카페", "cafe", 1);
        createCategoryIfAbsent("베이커리", "bakery", 2);
        createCategoryIfAbsent("외식", "restaurant", 3);
        createCategoryIfAbsent("영화·문화", "movie-culture", 4);
        createCategoryIfAbsent("뷰티", "beauty", 5);
        createCategoryIfAbsent("테마파크", "theme-park", 6);
    }

    private void seedTags() {
        createTagIfAbsent("무료", "free", 1);
        createTagIfAbsent("할인", "discount", 2);
        createTagIfAbsent("쿠폰", "coupon", 3);
        createTagIfAbsent("앱 필요", "app-required", 4);
        createTagIfAbsent("앱 불필요", "no-app-required", 5);
        createTagIfAbsent("회원가입 필요", "signup-required", 6);
        createTagIfAbsent("멤버십 필요", "membership-required", 7);
        createTagIfAbsent("조건 없음", "no-condition", 8);
        createTagIfAbsent("최소 구매 필요", "purchase-required", 9);
        createTagIfAbsent("생일 당일", "birthday-only", 10);
        createTagIfAbsent("생일월", "birthday-month", 11);
        createTagIfAbsent("생일 전후 7일", "birthday-week", 12);
        createTagIfAbsent("공식 확인", "officially-verified", 13);
        createTagIfAbsent("확인 필요", "needs-check", 14);
    }

    private void seedBrands() {
        createBrandIfAbsent("스타벅스", "starbucks", "cafe");
        createBrandIfAbsent("올리브영", "oliveyoung", "beauty");
        createBrandIfAbsent("CGV", "cgv", "movie-culture");
        createBrandIfAbsent("아웃백", "outback", "restaurant");
        createBrandIfAbsent("메가박스", "megabox", "movie-culture");
        createBrandIfAbsent("투썸플레이스", "twosome-place", "cafe");
        createBrandIfAbsent("파리바게뜨", "paris-baguette", "bakery");
        createBrandIfAbsent("롯데월드", "lotte-world", "theme-park");
    }

    private void createCategoryIfAbsent(String name, String slug, int displayOrder) {
        if (!categoryRepository.existsBySlug(slug)) {
            categoryRepository.save(new Category(name, slug, displayOrder));
        }
    }

    private void createTagIfAbsent(String name, String slug, int displayOrder) {
        if (!tagRepository.existsBySlug(slug)) {
            tagRepository.save(new Tag(name, slug, displayOrder));
        }
    }

    private void createBrandIfAbsent(String name, String slug, String categorySlug) {
        if (brandRepository.existsBySlug(slug)) {
            return;
        }

        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalStateException("Seed category not found: " + categorySlug));

        brandRepository.save(new Brand(category, name, slug));
    }
}
