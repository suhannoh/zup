package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.benefit.BenefitDetailItem;
import com.noh.zup.domain.benefit.BenefitDetailItemRepository;
import com.noh.zup.domain.benefit.BenefitRepository;
import com.noh.zup.domain.benefit.BenefitType;
import com.noh.zup.domain.benefit.BirthdayTimingType;
import com.noh.zup.domain.benefit.OccasionType;
import com.noh.zup.domain.benefit.PublicExpressionPolicy;
import com.noh.zup.domain.benefit.VerificationStatus;
import com.noh.zup.domain.source.BenefitSource;
import com.noh.zup.domain.source.BenefitSourceRepository;
import com.noh.zup.domain.verification.VerificationLog;
import com.noh.zup.domain.verification.VerificationLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BenefitCandidateService {

    private final BenefitCandidateRepository benefitCandidateRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitDetailItemRepository benefitDetailItemRepository;
    private final BenefitSourceRepository benefitSourceRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final CollectionRunRepository collectionRunRepository;

    public BenefitCandidateService(
            BenefitCandidateRepository benefitCandidateRepository,
            BenefitRepository benefitRepository,
            BenefitDetailItemRepository benefitDetailItemRepository,
            BenefitSourceRepository benefitSourceRepository,
            VerificationLogRepository verificationLogRepository,
            CollectionRunRepository collectionRunRepository
    ) {
        this.benefitCandidateRepository = benefitCandidateRepository;
        this.benefitRepository = benefitRepository;
        this.benefitDetailItemRepository = benefitDetailItemRepository;
        this.benefitSourceRepository = benefitSourceRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.collectionRunRepository = collectionRunRepository;
    }

    @Transactional(readOnly = true)
    public List<BenefitCandidateResponse> getCandidates(
            Long sourceWatchId,
            Long collectionRunId,
            BenefitCandidateStatus status,
            String keyword,
            Integer limit
    ) {
        String normalizedKeyword = normalize(keyword);
        List<BenefitCandidate> candidates = benefitCandidateRepository.findAllByOrderByIdDesc();
        Map<Long, Long> collectionRunIdBySnapshotId = getCollectionRunIdBySnapshotId(candidates);

        return candidates.stream()
                .filter(candidate -> sourceWatchId == null || candidate.getSourceWatch().getId().equals(sourceWatchId))
                .filter(candidate -> status == null || candidate.getStatus() == status)
                .filter(candidate -> matchesKeyword(candidate, normalizedKeyword))
                .filter(candidate -> collectionRunId == null
                        || collectionRunId.equals(resolveCollectionRunId(candidate, collectionRunIdBySnapshotId)))
                .limit(normalizeLimit(limit))
                .map(candidate -> BenefitCandidateResponse.from(
                        candidate,
                        resolveCollectionRunId(candidate, collectionRunIdBySnapshotId)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BenefitCandidateResponse getCandidate(Long id) {
        BenefitCandidate candidate = getCandidateEntity(id);
        Long collectionRunId = resolveCollectionRunId(candidate, getCollectionRunIdBySnapshotId(List.of(candidate)));
        return BenefitCandidateResponse.from(candidate, collectionRunId);
    }

    @Transactional
    public BenefitCandidateResponse updateStatus(Long id, BenefitCandidateStatusUpdateRequest request) {
        if (request.status() == BenefitCandidateStatus.DETECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "DETECTED cannot be set manually");
        }
        BenefitCandidate candidate = getCandidateEntity(id);
        candidate.updateStatus(request.status(), request.reviewMemo());
        Long collectionRunId = resolveCollectionRunId(candidate, getCollectionRunIdBySnapshotId(List.of(candidate)));
        return BenefitCandidateResponse.from(candidate, collectionRunId);
    }

    @Transactional
    public BenefitCandidateApproveResponse approve(Long id, BenefitCandidateApproveRequest request) {
        BenefitCandidate candidate = getCandidateEntity(id);
        validateApprovable(candidate);

        String title = firstText(request.title(), candidate.getTitle());
        String summary = firstText(request.summary(), candidate.getSummary());
        if (!StringUtils.hasText(title) || !StringUtils.hasText(summary)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Approved benefit title and summary are required");
        }

        VerificationStatus verificationStatus = VerificationStatus.VERIFIED;
        Benefit benefit = new Benefit(
                candidate.getBrand(),
                title,
                summary,
                firstValue(request.benefitType(), candidate.getBenefitType(), BenefitType.ETC)
        );
        benefit.update(
                null,
                null,
                null,
                candidate.getEvidenceText(),
                null,
                firstValue(request.occasionType(), candidate.getOccasionType(), OccasionType.BIRTHDAY),
                firstValue(request.birthdayTimingType(), candidate.getBirthdayTimingType(), BirthdayTimingType.UNKNOWN),
                buildConditionSummary(request, candidate),
                firstValue(request.requiresApp(), candidate.getRequiresApp(), false),
                firstValue(request.requiresMembership(), candidate.getRequiresMembership(), false),
                StringUtils.hasText(request.minimumPurchaseDescription()),
                null,
                request.birthdayTimingDescription(),
                null,
                null,
                null,
                verificationStatus,
                LocalDate.now(),
                true
        );
        if (PublicExpressionPolicy.containsProhibitedTerms(benefit)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Approved benefit contains prohibited public terms");
        }
        Benefit savedBenefit = benefitRepository.save(benefit);
        saveDetailItems(savedBenefit, request.detailItems());

        SourceWatch sourceWatch = candidate.getSourceWatch();
        BenefitSource source = new BenefitSource(savedBenefit, sourceWatch.getSourceType(), sourceWatch.getUrl());
        source.update(
                null,
                null,
                sourceWatch.getTitle(),
                LocalDate.now(),
                "자동 수집 후보 승인으로 생성됨. evidence: " + truncate(candidate.getEvidenceText(), 300)
        );
        source.updateVerificationMetadata(
                sourceWatch.getUrl(),
                LocalDate.now(),
                CollectionMethod.AUTO_COLLECTED,
                "허용된 공식 출처에서 자동 후보 생성 후 관리자 검수",
                null
        );
        benefitSourceRepository.save(source);

        verificationLogRepository.save(new VerificationLog(
                savedBenefit,
                VerificationStatus.DRAFT,
                verificationStatus,
                "자동 수집 후보 승인으로 Benefit 생성. candidateId=" + candidate.getId(),
                null,
                LocalDateTime.now()
        ));

        candidate.approve(savedBenefit, firstText(request.adminMemo(), candidate.getReviewMemo()));
        return new BenefitCandidateApproveResponse(
                candidate.getId(),
                savedBenefit.getId(),
                verificationStatus,
                "benefit created from candidate"
        );
    }

    private void saveDetailItems(Benefit benefit, List<BenefitDetailItemApproveRequest> detailItems) {
        if (detailItems == null || detailItems.isEmpty()) {
            return;
        }
        int fallbackOrder = 1;
        for (BenefitDetailItemApproveRequest item : detailItems) {
            if (!StringUtils.hasText(item.title())) {
                fallbackOrder++;
                continue;
            }
            Integer displayOrder = item.displayOrder() == null ? fallbackOrder : item.displayOrder();
            benefitDetailItemRepository.save(new BenefitDetailItem(
                    benefit,
                    normalize(item.brandName()),
                    item.title().trim(),
                    normalize(item.description()),
                    normalize(item.conditionText()),
                    normalize(item.imageUrl()),
                    displayOrder
            ));
            fallbackOrder++;
        }
    }

    private BenefitCandidate getCandidateEntity(Long id) {
        return benefitCandidateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "BenefitCandidate not found"));
    }

    private Map<Long, Long> getCollectionRunIdBySnapshotId(List<BenefitCandidate> candidates) {
        Set<Long> snapshotIds = candidates.stream()
                .map(candidate -> candidate.getSnapshot().getId())
                .collect(Collectors.toSet());
        if (snapshotIds.isEmpty()) {
            return Map.of();
        }
        return collectionRunRepository.findBySnapshotIdIn(snapshotIds).stream()
                .filter(run -> run.getSnapshotId() != null)
                .collect(Collectors.toMap(
                        CollectionRun::getSnapshotId,
                        CollectionRun::getId,
                        (existing, ignored) -> existing
                ));
    }

    private Long resolveCollectionRunId(BenefitCandidate candidate, Map<Long, Long> collectionRunIdBySnapshotId) {
        if (candidate.getCollectionRunId() != null) {
            return candidate.getCollectionRunId();
        }
        return collectionRunIdBySnapshotId.get(candidate.getSnapshot().getId());
    }

    private boolean matchesKeyword(BenefitCandidate candidate, String keyword) {
        if (keyword == null) {
            return true;
        }
        return contains(candidate.getTitle(), keyword)
                || contains(candidate.getSummary(), keyword)
                || contains(candidate.getBrand().getName(), keyword)
                || contains(candidate.getSourceWatch().getTitle(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 100;
        }
        return Math.min(limit, 200);
    }

    private void validateApprovable(BenefitCandidate candidate) {
        if (candidate.getStatus() == BenefitCandidateStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Rejected candidate cannot be approved");
        }
        if (candidate.getApprovedBenefit() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Candidate is already approved");
        }
        SourceWatch sourceWatch = candidate.getSourceWatch();
        if (!Boolean.TRUE.equals(sourceWatch.getIsActive())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Inactive source watch cannot be approved");
        }
        if (!StringUtils.hasText(sourceWatch.getUrl())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Official source URL is required");
        }
    }

    private String buildConditionSummary(BenefitCandidateApproveRequest request, BenefitCandidate candidate) {
        String usageCondition = firstText(request.usageCondition(), firstText(candidate.getUsageGuideText(), candidate.getEvidenceText()));
        StringBuilder builder = new StringBuilder(truncate(usageCondition, 400));
        if (Boolean.TRUE.equals(request.requiresSignup()) || Boolean.TRUE.equals(candidate.getRequiresSignup())) {
            builder.append(" 회원가입 필요.");
        }
        if (StringUtils.hasText(request.minimumPurchaseDescription())) {
            builder.append(" 최소 구매 조건: ").append(request.minimumPurchaseDescription());
        }
        return truncate(builder.toString().trim(), 500);
    }

    private String firstText(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private <T> T firstValue(T primary, T fallback, T defaultValue) {
        if (primary != null) {
            return primary;
        }
        return fallback != null ? fallback : defaultValue;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
