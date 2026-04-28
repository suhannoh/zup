package com.noh.zup.domain.benefit;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.source.BenefitSourceRepository;
import com.noh.zup.domain.tag.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final BenefitTagRepository benefitTagRepository;
    private final BenefitSourceRepository benefitSourceRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public BenefitService(
            BenefitRepository benefitRepository,
            BenefitTagRepository benefitTagRepository,
            BenefitSourceRepository benefitSourceRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository
    ) {
        this.benefitRepository = benefitRepository;
        this.benefitTagRepository = benefitTagRepository;
        this.benefitSourceRepository = benefitSourceRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<BenefitListResponse> getBenefits(BenefitSearchCondition condition) {
        BenefitSearchCondition normalized = normalize(condition);
        validateSearchSlugs(normalized);

        return benefitRepository.searchPublishedBenefits(
                        VerificationStatus.PUBLISHED,
                        normalized.brandSlug(),
                        normalized.categorySlug(),
                        normalized.tagSlug(),
                        normalized.benefitType(),
                        normalized.birthdayTimingType(),
                        normalized.requiredApp(),
                        normalized.requiredMembership(),
                        normalized.requiredPurchase()
                ).stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BenefitListResponse> getPublishedBenefitsByBrandSlug(String brandSlug) {
        return benefitRepository
                .findAllByBrandSlugAndIsActiveTrueAndVerificationStatusOrderByCreatedAtDesc(
                        brandSlug,
                        VerificationStatus.PUBLISHED
                ).stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BenefitListResponse> getBenefitsByTag(String tagSlug) {
        tagRepository.findBySlugAndIsActiveTrue(tagSlug)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Tag not found"));

        return getBenefits(new BenefitSearchCondition(
                null,
                null,
                tagSlug,
                null,
                null,
                null,
                null,
                null
        ));
    }

    @Transactional(readOnly = true)
    public BenefitDetailResponse getBenefitDetail(Long id) {
        Benefit benefit = benefitRepository
                .findByIdAndIsActiveTrueAndVerificationStatus(id, VerificationStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found"));

        return BenefitDetailResponse.of(benefit, getTags(benefit), getSources(benefit));
    }

    private BenefitListResponse toListResponse(Benefit benefit) {
        return BenefitListResponse.of(benefit, getTags(benefit), getSources(benefit));
    }

    private List<BenefitTagResponse> getTags(Benefit benefit) {
        return benefitTagRepository.findAllByBenefitId(benefit.getId()).stream()
                .map(BenefitTag::getTag)
                .map(BenefitTagResponse::from)
                .toList();
    }

    private List<BenefitSourceResponse> getSources(Benefit benefit) {
        return benefitSourceRepository.findAllByBenefitIdAndIsActiveTrue(benefit.getId()).stream()
                .map(BenefitSourceResponse::from)
                .toList();
    }

    private BenefitSearchCondition normalize(BenefitSearchCondition condition) {
        return new BenefitSearchCondition(
                normalize(condition.brandSlug()),
                normalize(condition.categorySlug()),
                normalize(condition.tagSlug()),
                condition.benefitType(),
                condition.birthdayTimingType(),
                condition.requiredApp(),
                condition.requiredMembership(),
                condition.requiredPurchase()
        );
    }

    private void validateSearchSlugs(BenefitSearchCondition condition) {
        if (condition.brandSlug() != null && brandRepository.findBySlugAndIsActiveTrue(condition.brandSlug()).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Brand not found");
        }
        if (condition.categorySlug() != null && categoryRepository.findBySlugAndIsActiveTrue(condition.categorySlug()).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Category not found");
        }
        if (condition.tagSlug() != null && tagRepository.findBySlugAndIsActiveTrue(condition.tagSlug()).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Tag not found");
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
