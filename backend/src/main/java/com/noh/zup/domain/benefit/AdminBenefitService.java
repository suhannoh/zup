package com.noh.zup.domain.benefit;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.category.CategoryRepository;
import com.noh.zup.domain.source.BenefitSourceRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBenefitService {

    private final BenefitRepository benefitRepository;
    private final BenefitTagRepository benefitTagRepository;
    private final BenefitSourceRepository benefitSourceRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final UserReportRepository userReportRepository;

    public AdminBenefitService(
            BenefitRepository benefitRepository,
            BenefitTagRepository benefitTagRepository,
            BenefitSourceRepository benefitSourceRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            VerificationLogRepository verificationLogRepository,
            UserReportRepository userReportRepository
    ) {
        this.benefitRepository = benefitRepository;
        this.benefitTagRepository = benefitTagRepository;
        this.benefitSourceRepository = benefitSourceRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.userReportRepository = userReportRepository;
    }

    @Transactional(readOnly = true)
    public List<BenefitDetailResponse> getBenefits(
            String brandSlug,
            String categorySlug,
            VerificationStatus verificationStatus,
            BenefitType benefitType,
            BirthdayTimingType birthdayTimingType,
            Boolean isActive,
            String keyword
    ) {
        String normalizedBrandSlug = normalize(brandSlug);
        String normalizedCategorySlug = normalize(categorySlug);
        String normalizedKeyword = normalize(keyword);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);

        return benefitRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(benefit -> normalizedBrandSlug == null
                        || benefit.getBrand().getSlug().equals(normalizedBrandSlug))
                .filter(benefit -> normalizedCategorySlug == null
                        || benefit.getBrand().getCategory().getSlug().equals(normalizedCategorySlug))
                .filter(benefit -> verificationStatus == null
                        || benefit.getVerificationStatus() == verificationStatus)
                .filter(benefit -> benefitType == null || benefit.getBenefitType() == benefitType)
                .filter(benefit -> birthdayTimingType == null
                        || benefit.getBirthdayTimingType() == birthdayTimingType)
                .filter(benefit -> isActive == null || benefit.getIsActive().equals(isActive))
                .filter(benefit -> lowerKeyword == null
                        || benefit.getTitle().toLowerCase(Locale.ROOT).contains(lowerKeyword)
                        || benefit.getSummary().toLowerCase(Locale.ROOT).contains(lowerKeyword))
                .map(this::toDetailResponse)
                .toList();
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
                userReportRepository.countByStatus(ReportStatus.RESOLVED)
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
                        .toList()
        );
    }

    private Benefit getBenefitEntity(Long id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found"));
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
}
