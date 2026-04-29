package com.noh.zup.domain.benefit;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.common.response.PageResponse;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.collection.CollectionDashboardService;
import com.noh.zup.domain.collection.CollectionMethod;
import com.noh.zup.domain.source.BenefitSourceRepository;
import com.noh.zup.domain.source.BenefitSource;
import com.noh.zup.domain.tag.Tag;
import com.noh.zup.domain.tag.TagRepository;
import com.noh.zup.domain.report.ReportStatus;
import com.noh.zup.domain.report.UserReportRepository;
import com.noh.zup.domain.verification.VerificationLog;
import com.noh.zup.domain.verification.VerificationLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBenefitService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final BenefitRepository benefitRepository;
    private final BenefitTagRepository benefitTagRepository;
    private final BenefitSourceRepository benefitSourceRepository;
    private final BenefitDetailItemRepository benefitDetailItemRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final UserReportRepository userReportRepository;
    private final CollectionDashboardService collectionDashboardService;

    public AdminBenefitService(
            BenefitRepository benefitRepository,
            BenefitTagRepository benefitTagRepository,
            BenefitSourceRepository benefitSourceRepository,
            BenefitDetailItemRepository benefitDetailItemRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            VerificationLogRepository verificationLogRepository,
            UserReportRepository userReportRepository,
            CollectionDashboardService collectionDashboardService
    ) {
        this.benefitRepository = benefitRepository;
        this.benefitTagRepository = benefitTagRepository;
        this.benefitSourceRepository = benefitSourceRepository;
        this.benefitDetailItemRepository = benefitDetailItemRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.userReportRepository = userReportRepository;
        this.collectionDashboardService = collectionDashboardService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BenefitSummaryResponse> getBenefits(
            String brandSlug,
            String categorySlug,
            VerificationStatus verificationStatus,
            BenefitType benefitType,
            BirthdayTimingType birthdayTimingType,
            Boolean isActive,
            String keyword,
            Integer page,
            Integer size
    ) {
        String normalizedBrandSlug = normalize(brandSlug);
        String normalizedCategorySlug = normalize(categorySlug);
        String normalizedKeyword = normalize(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);
        Page<Benefit> benefits = benefitRepository.searchAdminBenefits(
                normalizedBrandSlug,
                normalizedCategorySlug,
                verificationStatus,
                benefitType,
                birthdayTimingType,
                isActive,
                lowerKeyword,
                PageRequest.of(normalizePage(page), normalizeSize(size))
        );
        return PageResponse.from(benefits, benefits.stream().map(this::toSummaryResponse).toList());
    }

    @Transactional(readOnly = true)
    public BenefitDetailResponse getBenefit(Long id) {
        return toDetailResponse(getBenefitEntity(id));
    }

    @Transactional
    public BenefitDetailResponse createBenefit(AdminBenefitCreateRequest request) {
        Brand brand = getBrand(request.brandId());
        Benefit benefit = new Benefit(brand, request.title(), request.summary(), request.benefitType());
        benefit.update(
                null,
                null,
                null,
                request.detail(),
                null,
                request.occasionType(),
                request.birthdayTimingType(),
                request.conditionSummary(),
                request.requiredApp(),
                request.requiredMembership(),
                request.requiredPurchase(),
                request.membershipGrade(),
                request.usagePeriodDescription(),
                request.availableFrom(),
                request.availableTo(),
                request.caution(),
                request.verificationStatus(),
                request.lastVerifiedAt(),
                request.isActive()
        );
        return toDetailResponse(benefitRepository.save(benefit));
    }

    @Transactional
    public BenefitDetailResponse createManualBenefit(AdminManualBenefitCreateRequest request) {
        Brand brand = getBrand(request.brandId());
        VerificationStatus verificationStatus = request.verificationStatus() == null
                ? VerificationStatus.VERIFIED
                : request.verificationStatus();
        boolean isActive = request.isActive() == null || request.isActive();
        validateManualBenefitRequest(request, verificationStatus, isActive);

        Benefit benefit = new Benefit(brand, request.title(), request.summary(), request.benefitType());
        benefit.update(
                null,
                null,
                null,
                request.detail(),
                null,
                request.occasionType() == null ? OccasionType.BIRTHDAY : request.occasionType(),
                request.birthdayTimingType() == null ? BirthdayTimingType.UNKNOWN : request.birthdayTimingType(),
                request.conditionSummary(),
                request.requiredApp(),
                request.requiredMembership(),
                request.requiredPurchase(),
                request.membershipGrade(),
                request.usagePeriodDescription(),
                request.availableFrom(),
                request.availableTo(),
                request.caution(),
                verificationStatus,
                resolveManualLastVerifiedAt(request),
                isActive
        );
        Benefit savedBenefit = benefitRepository.save(benefit);

        saveManualDetailItems(savedBenefit, request.detailItems());
        saveManualSources(savedBenefit, request.sources());

        verificationLogRepository.save(new VerificationLog(
                savedBenefit,
                VerificationStatus.DRAFT,
                verificationStatus,
                "수동 혜택 등록. 관리자가 공식 출처를 직접 확인함.",
                null,
                LocalDateTime.now()
        ));

        return toDetailResponse(savedBenefit);
    }

    @Transactional
    public BenefitDetailResponse updateBenefit(Long id, AdminBenefitUpdateRequest request) {
        Benefit benefit = getBenefitEntity(id);
        Brand brand = request.brandId() == null ? null : getBrand(request.brandId());
        benefit.update(
                brand,
                request.title(),
                request.summary(),
                request.detail(),
                request.benefitType(),
                request.occasionType(),
                request.birthdayTimingType(),
                request.conditionSummary(),
                request.requiredApp(),
                request.requiredMembership(),
                request.requiredPurchase(),
                request.membershipGrade(),
                request.usagePeriodDescription(),
                request.availableFrom(),
                request.availableTo(),
                request.caution(),
                request.verificationStatus(),
                request.lastVerifiedAt(),
                request.isActive()
        );
        return toDetailResponse(benefit);
    }

    @Transactional
    public BenefitDetailResponse updateStatus(Long id, AdminBenefitStatusUpdateRequest request) {
        Benefit benefit = getBenefitEntity(id);
        VerificationStatus beforeStatus = benefit.getVerificationStatus();
        LocalDate lastVerifiedAt = request.lastVerifiedAt();
        if (request.verificationStatus() == VerificationStatus.PUBLISHED && lastVerifiedAt == null) {
            lastVerifiedAt = LocalDate.now();
        }
        if (request.verificationStatus() == VerificationStatus.PUBLISHED && PublicExpressionPolicy.containsProhibitedTerms(benefit)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Public expression contains prohibited terms");
        }
        benefit.changeStatus(request.verificationStatus(), lastVerifiedAt);
        if (beforeStatus != request.verificationStatus()) {
            verificationLogRepository.save(new VerificationLog(
                    benefit,
                    beforeStatus,
                    request.verificationStatus(),
                    request.memo(),
                    null,
                    LocalDateTime.now()
            ));
        }
        return toDetailResponse(benefit);
    }

    @Transactional
    public BenefitDetailResponse updateActive(Long id, Boolean isActive) {
        Benefit benefit = getBenefitEntity(id);
        benefit.changeActive(isActive);
        return toDetailResponse(benefit);
    }

    @Transactional
    public BenefitDetailResponse addTag(Long benefitId, Long tagId) {
        Benefit benefit = getBenefitEntity(benefitId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Tag not found"));
        if (!benefitTagRepository.existsByBenefitIdAndTagId(benefitId, tagId)) {
            benefitTagRepository.save(new BenefitTag(benefit, tag));
        }
        return toDetailResponse(benefit);
    }

    @Transactional
    public BenefitDetailResponse removeTag(Long benefitId, Long tagId) {
        Benefit benefit = getBenefitEntity(benefitId);
        benefitTagRepository.findByBenefitIdAndTagId(benefitId, tagId)
                .ifPresent(benefitTagRepository::delete);
        return toDetailResponse(benefit);
    }

    @Transactional(readOnly = true)
    public List<BenefitDetailResponse> getReviewNeededBenefits() {
        return benefitRepository
                .findAllByVerificationStatusInOrderByUpdatedAtDesc(List.of(
                        VerificationStatus.NEEDS_CHECK,
                        VerificationStatus.DRAFT
                )).stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BenefitDetailResponse> getStaleBenefits(int days) {
        LocalDate threshold = LocalDate.now().minusDays(days);
        Comparator<Benefit> nullsFirstByLastVerifiedAt = Comparator.comparing(
                Benefit::getLastVerifiedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())
        );

        return benefitRepository
                .findAllByIsActiveTrueAndVerificationStatusIn(List.of(
                        VerificationStatus.PUBLISHED,
                        VerificationStatus.VERIFIED
                )).stream()
                .filter(benefit -> benefit.getLastVerifiedAt() == null || !benefit.getLastVerifiedAt().isAfter(threshold))
                .sorted(nullsFirstByLastVerifiedAt)
                .map(this::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long staleBenefitCount = getStaleBenefits(90).size();
        return new AdminDashboardResponse(
                brandRepository.countByIsActiveTrue(),
                benefitRepository.countByVerificationStatusAndIsActiveTrue(VerificationStatus.PUBLISHED),
                benefitRepository.countByVerificationStatus(VerificationStatus.DRAFT),
                benefitRepository.countByVerificationStatus(VerificationStatus.NEEDS_CHECK),
                benefitRepository.countByVerificationStatus(VerificationStatus.EXPIRED),
                staleBenefitCount,
                categoryRepository.count(),
                tagRepository.count(),
                userReportRepository.countByStatus(ReportStatus.RECEIVED),
                userReportRepository.countByStatus(ReportStatus.REVIEWING),
                userReportRepository.countByStatus(ReportStatus.RESOLVED),
                collectionDashboardService.getSummary()
        );
    }

    private BenefitDetailResponse toDetailResponse(Benefit benefit) {
        return BenefitDetailResponse.of(
                benefit,
                benefitTagRepository.findAllByBenefitId(benefit.getId()).stream()
                        .map(BenefitTag::getTag)
                        .map(BenefitTagResponse::from)
                        .toList(),
                benefitSourceRepository.findAllByBenefitId(benefit.getId()).stream()
                        .map(BenefitSourceResponse::from)
                        .toList(),
                benefitDetailItemRepository.findAllByBenefitIdOrderByDisplayOrderAscIdAsc(benefit.getId()).stream()
                        .map(BenefitDetailItemResponse::from)
                        .toList()
        );
    }

    private BenefitSummaryResponse toSummaryResponse(Benefit benefit) {
        return BenefitSummaryResponse.of(
                benefit,
                benefitDetailItemRepository.countByBenefitId(benefit.getId()),
                benefitSourceRepository.countByBenefitId(benefit.getId()),
                benefitTagRepository.countByBenefitId(benefit.getId())
        );
    }

    private Benefit getBenefitEntity(Long id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found"));
    }

    private void validateManualBenefitRequest(
            AdminManualBenefitCreateRequest request,
            VerificationStatus verificationStatus,
            boolean isActive
    ) {
        if (request.sources() == null || request.sources().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "At least one official source is required");
        }
        if (verificationStatus == VerificationStatus.PUBLISHED) {
            if (!isActive) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Published manual benefit must be active");
            }
            if (resolveManualLastVerifiedAt(request) == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Published manual benefit requires last verified date");
            }
            Benefit probe = new Benefit(getBrand(request.brandId()), request.title(), request.summary(), request.benefitType());
            probe.update(
                    null,
                    null,
                    null,
                    request.detail(),
                    null,
                    request.occasionType(),
                    request.birthdayTimingType(),
                    request.conditionSummary(),
                    request.requiredApp(),
                    request.requiredMembership(),
                    request.requiredPurchase(),
                    request.membershipGrade(),
                    request.usagePeriodDescription(),
                    request.availableFrom(),
                    request.availableTo(),
                    request.caution(),
                    verificationStatus,
                    resolveManualLastVerifiedAt(request),
                    isActive
            );
            if (PublicExpressionPolicy.containsProhibitedTerms(probe) || containsManualPublishBlockedTerm(request)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Published manual benefit contains blocked terms");
            }
        }
    }

    private boolean containsManualPublishBlockedTerm(AdminManualBenefitCreateRequest request) {
        String text = String.join("\n",
                value(request.title()),
                value(request.summary()),
                value(request.detail()),
                value(request.conditionSummary()),
                value(request.usagePeriodDescription()),
                value(request.caution())
        );
        return List.of("확인 필요", "임시", "테스트", "TODO", "미정").stream().anyMatch(text::contains);
    }

    private LocalDate resolveManualLastVerifiedAt(AdminManualBenefitCreateRequest request) {
        if (request.lastVerifiedAt() != null) {
            return request.lastVerifiedAt();
        }
        if (request.sources() == null || request.sources().isEmpty()) {
            return null;
        }
        return request.sources().stream()
                .map(AdminManualBenefitCreateRequest.ManualBenefitSourceRequest::sourceCheckedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private void saveManualDetailItems(
            Benefit benefit,
            List<AdminManualBenefitCreateRequest.ManualBenefitDetailItemRequest> detailItems
    ) {
        if (detailItems == null || detailItems.isEmpty()) {
            return;
        }
        int fallbackOrder = 1;
        for (AdminManualBenefitCreateRequest.ManualBenefitDetailItemRequest item : detailItems) {
            if (!StringUtils.hasText(item.title())) {
                fallbackOrder++;
                continue;
            }
            BenefitDetailItem saved = benefitDetailItemRepository.save(new BenefitDetailItem(
                    benefit,
                    normalize(item.brandName()),
                    item.title().trim(),
                    normalize(item.description()),
                    normalize(item.conditionText()),
                    normalize(item.imageUrl()),
                    item.displayOrder() == null ? fallbackOrder : item.displayOrder()
            ));
            if (item.isActive() != null) {
                saved.changeActive(item.isActive());
            }
            fallbackOrder++;
        }
    }

    private void saveManualSources(
            Benefit benefit,
            List<AdminManualBenefitCreateRequest.ManualBenefitSourceRequest> sources
    ) {
        for (AdminManualBenefitCreateRequest.ManualBenefitSourceRequest request : sources) {
            BenefitSource source = new BenefitSource(benefit, request.sourceType(), request.sourceUrl());
            source.update(
                    null,
                    null,
                    normalize(request.sourceTitle()),
                    request.sourceCheckedAt(),
                    normalize(request.memo())
            );
            source.updateVerificationMetadata(
                    request.sourceUrl(),
                    request.sourceCheckedAt(),
                    request.collectionMethod() == null ? CollectionMethod.MANUAL_VERIFIED : request.collectionMethod(),
                    normalize(request.verificationSummary()),
                    normalize(request.sourceNotice())
            );
            benefitSourceRepository.save(source);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private Brand getBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Brand not found"));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
