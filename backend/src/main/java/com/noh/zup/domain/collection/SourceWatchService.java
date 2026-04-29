package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.collection.candidate.BenefitCandidateDetector;
import com.noh.zup.domain.collection.candidate.BenefitCandidateDetectionResult;
import com.noh.zup.domain.collection.extract.ExtractedText;
import com.noh.zup.domain.collection.extract.HtmlTextExtractor;
import com.noh.zup.domain.collection.fetch.FetchResult;
import com.noh.zup.domain.collection.fetch.OfficialSourceFetcher;
import com.noh.zup.domain.collection.robots.RobotsTxtCheckResult;
import com.noh.zup.domain.collection.robots.RobotsTxtChecker;
import com.noh.zup.domain.collection.scheduler.CollectionLock;
import com.noh.zup.domain.collection.scheduler.CollectionRedisLock;
import com.noh.zup.domain.source.SourceType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceWatchService {

    private final SourceWatchRepository sourceWatchRepository;
    private final PageSnapshotRepository pageSnapshotRepository;
    private final BrandRepository brandRepository;
    private final CollectionRunRepository collectionRunRepository;
    private final BenefitCandidateRepository benefitCandidateRepository;
    private final OfficialSourceFetcher officialSourceFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final HtmlTextExtractor htmlTextExtractor;
    private final BenefitCandidateDetector benefitCandidateDetector;
    private final CollectionRedisLock collectionRedisLock;
    private final long minDomainIntervalSeconds;
    private final long lockTtlSeconds;

    public SourceWatchService(
            SourceWatchRepository sourceWatchRepository,
            PageSnapshotRepository pageSnapshotRepository,
            BrandRepository brandRepository,
            CollectionRunRepository collectionRunRepository,
            BenefitCandidateRepository benefitCandidateRepository,
            OfficialSourceFetcher officialSourceFetcher,
            RobotsTxtChecker robotsTxtChecker,
            HtmlTextExtractor htmlTextExtractor,
            BenefitCandidateDetector benefitCandidateDetector,
            CollectionRedisLock collectionRedisLock,
            @Value("${app.crawler.min-domain-interval-seconds:60}") long minDomainIntervalSeconds,
            @Value("${app.collection.scheduler.lock-ttl-seconds:120}") long lockTtlSeconds
    ) {
        this.sourceWatchRepository = sourceWatchRepository;
        this.pageSnapshotRepository = pageSnapshotRepository;
        this.brandRepository = brandRepository;
        this.collectionRunRepository = collectionRunRepository;
        this.benefitCandidateRepository = benefitCandidateRepository;
        this.officialSourceFetcher = officialSourceFetcher;
        this.robotsTxtChecker = robotsTxtChecker;
        this.htmlTextExtractor = htmlTextExtractor;
        this.benefitCandidateDetector = benefitCandidateDetector;
        this.collectionRedisLock = collectionRedisLock;
        this.minDomainIntervalSeconds = minDomainIntervalSeconds;
        this.lockTtlSeconds = lockTtlSeconds;
    }

    @Transactional(readOnly = true)
    public List<SourceWatchResponse> getSourceWatches() {
        List<SourceWatch> sourceWatches = sourceWatchRepository.findAllByOrderByIdDesc();
        Map<Long, RecentCollectionRunSummaryResponse> recentRunsBySourceWatchId = getRecentRunsBySourceWatchId(sourceWatches);
        return sourceWatches.stream()
                .map(sourceWatch -> SourceWatchResponse.from(
                        sourceWatch,
                        recentRunsBySourceWatchId.get(sourceWatch.getId())
                ))
                .toList();
    }

    @Transactional
    public SourceWatchResponse createSourceWatch(SourceWatchCreateRequest request) {
        validateOfficialSourceType(request.sourceType());
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Brand not found"));
        SourceWatch sourceWatch = new SourceWatch(
                brand,
                request.sourceType(),
                request.title(),
                request.url(),
                request.isActive()
        );
        sourceWatch.updatePolicy(
                request.robotsCheckStatus(),
                request.termsCheckStatus(),
                request.collectionMethod(),
                request.loginRequired(),
                null,
                request.policyCheckNote(),
                LocalDateTime.now(),
                request.manualVerificationNote(),
                null
        );
        return SourceWatchResponse.from(sourceWatchRepository.save(sourceWatch), null);
    }

    @Transactional
    public SourceWatchResponse updateSourceWatch(Long id, SourceWatchUpdateRequest request) {
        SourceWatch sourceWatch = getSourceWatch(id);
        if (request.sourceType() != null) {
            validateOfficialSourceType(request.sourceType());
        }
        validateManualPermissionStatus(request.collectionPermissionStatus());
        sourceWatch.update(request.sourceType(), request.title(), request.url(), request.isActive());
        CollectionPermissionStatus before = sourceWatch.getCollectionPermissionStatus();
        sourceWatch.updatePolicy(
                request.robotsCheckStatus(),
                request.termsCheckStatus(),
                request.collectionMethod(),
                request.loginRequired(),
                request.collectionPermissionStatus(),
                request.policyCheckNote(),
                LocalDateTime.now(),
                request.manualVerificationNote(),
                request.manualVerificationNote() == null ? null : LocalDateTime.now()
        );
        if (becameBlocked(before, sourceWatch.getCollectionPermissionStatus())) {
            benefitCandidateRepository.markUnapprovedAsNeedsManualReview(sourceWatch.getId());
        }
        return SourceWatchResponse.from(sourceWatch, getRecentRun(sourceWatch.getId()));
    }

    @Transactional
    public SourceWatchResponse updateActive(Long id, Boolean isActive) {
        SourceWatch sourceWatch = getSourceWatch(id);
        sourceWatch.changeActive(isActive);
        return SourceWatchResponse.from(sourceWatch, getRecentRun(sourceWatch.getId()));
    }

    @Transactional
    public SourceWatchCollectResponse collect(Long id) {
        return collect(id, CollectionTriggerType.MANUAL);
    }

    @Transactional
    public SourceWatchRegenerateCandidatesResponse regenerateCandidates(Long id) {
        SourceWatch sourceWatch = getSourceWatch(id);
        CollectionRun collectionRun = collectionRunRepository.save(
                new CollectionRun(sourceWatch, CollectionTriggerType.MANUAL_REGENERATE_CANDIDATES)
        );
        SkippedReason skippedReason = getPolicySkippedReason(sourceWatch);
        if (skippedReason != null) {
            sourceWatch.markSkipped();
            collectionRun.completeSkipped(
                    skippedReason.name(),
                    "정책상 자동 수집 및 후보 재생성이 차단된 출처입니다. 공식 페이지를 직접 확인한 뒤 수동으로 등록하거나 수정해 주세요."
            );
            if (sourceWatch.getCollectionPermissionStatus() == CollectionPermissionStatus.BLOCKED_BY_ROBOTS
                    || sourceWatch.getCollectionPermissionStatus() == CollectionPermissionStatus.BLOCKED_BY_TERMS) {
                benefitCandidateRepository.markUnapprovedAsNeedsManualReview(sourceWatch.getId());
            }
            return new SourceWatchRegenerateCandidatesResponse(
                    sourceWatch.getId(),
                    collectionRun.getId(),
                    null,
                    0,
                    0,
                    skippedReason.name(),
                    "정책상 후보 재생성이 차단되었습니다."
            );
        }
        Optional<PageSnapshot> snapshot = pageSnapshotRepository.findTopBySourceWatchIdOrderByFetchedAtDescIdDesc(id);
        if (snapshot.isEmpty()) {
            collectionRun.completeSkipped("SNAPSHOT_NOT_FOUND", "재생성할 스냅샷이 없습니다.");
            return new SourceWatchRegenerateCandidatesResponse(
                    sourceWatch.getId(),
                    collectionRun.getId(),
                    null,
                    0,
                    0,
                    "SNAPSHOT_NOT_FOUND",
                    "재생성할 스냅샷이 없습니다."
            );
        }

        try {
            BenefitCandidateDetectionResult result = benefitCandidateDetector.detectWithResult(
                    sourceWatch,
                    snapshot.get(),
                    collectionRun.getId()
            );
            collectionRun.completeSuccess(false, false, result.createdCandidateCount(), snapshot.get().getId());
            return new SourceWatchRegenerateCandidatesResponse(
                    sourceWatch.getId(),
                    collectionRun.getId(),
                    snapshot.get().getId(),
                    result.createdCandidateCount(),
                    result.skippedDuplicateCount(),
                    null,
                    "candidate regeneration completed"
            );
        } catch (RuntimeException exception) {
            collectionRun.completeFailed(false, "UNKNOWN", exception.getMessage());
            return new SourceWatchRegenerateCandidatesResponse(
                    sourceWatch.getId(),
                    collectionRun.getId(),
                    snapshot.get().getId(),
                    0,
                    0,
                    "UNKNOWN",
                    "후보 재생성 중 오류가 발생했습니다."
            );
        }
    }

    @Transactional
    public SourceWatchCollectResponse collect(Long id, CollectionTriggerType triggerType) {
        SourceWatch sourceWatch = getSourceWatch(id);
        Optional<CollectionLock> lock = Optional.empty();
        if (triggerType == CollectionTriggerType.MANUAL) {
            lock = collectionRedisLock.tryLock(id, Duration.ofSeconds(lockTtlSeconds));
            if (lock.isEmpty()) {
                return createSkippedResponse(sourceWatch, triggerType, "COLLECTION_ALREADY_RUNNING", "같은 SourceWatch 수집이 이미 진행 중입니다.");
            }
        }
        try {
            if (!Boolean.TRUE.equals(sourceWatch.getIsActive())) {
                return createSkippedResponse(sourceWatch, triggerType, "SOURCE_WATCH_INACTIVE", "비활성 SourceWatch라 수집을 건너뛰었습니다.");
            }

            SkippedReason policySkippedReason = getPolicySkippedReason(sourceWatch);
            if (policySkippedReason != null) {
                return createSkippedResponse(sourceWatch, triggerType, policySkippedReason.name(), "정책상 자동 수집 대상이 아니므로 건너뛰었습니다.");
            }

            DomainRateLimitResult domainRateLimitResult = checkDomainRateLimit(sourceWatch);
            if (domainRateLimitResult.rateLimited()) {
                return createSkippedResponse(
                        sourceWatch,
                        triggerType,
                        "RATE_LIMITED_BY_DOMAIN",
                        "같은 도메인의 최근 수집 이후 최소 수집 간격이 지나지 않아 수집을 건너뛰었습니다."
                );
            }

            CollectionRun collectionRun = collectionRunRepository.save(new CollectionRun(sourceWatch, triggerType));
            try {
                RobotsTxtCheckResult robotsTxtCheckResult = robotsTxtChecker.check(sourceWatch.getUrl());
                sourceWatch.updateRobotsCheckStatus(toRobotsCheckStatus(robotsTxtCheckResult), buildRobotsErrorMessage(robotsTxtCheckResult));
                if (!robotsTxtCheckResult.allowed()) {
                    sourceWatch.markSkipped();
                    if (sourceWatch.getCollectionPermissionStatus() == CollectionPermissionStatus.BLOCKED_BY_ROBOTS) {
                        benefitCandidateRepository.markUnapprovedAsNeedsManualReview(sourceWatch.getId());
                    }
                    String errorMessage = buildRobotsErrorMessage(robotsTxtCheckResult);
                    collectionRun.completeSkipped(robotsTxtCheckResult.failureReason(), errorMessage);
                    return new SourceWatchCollectResponse(
                            id,
                            false,
                            false,
                            0,
                            robotsTxtCheckResult.failureReason(),
                            robotsTxtCheckResult.message()
                    );
                }
                if (sourceWatch.getCollectionPermissionStatus() != CollectionPermissionStatus.ALLOWED_TO_COLLECT) {
                    collectionRun.completeSkipped(SkippedReason.COLLECTION_PERMISSION_NOT_APPROVED.name(), "robots.txt 확인 후 정책상 자동 수집 대상이 아니므로 건너뛰었습니다.");
                    sourceWatch.markSkipped();
                    return new SourceWatchCollectResponse(id, false, false, 0, SkippedReason.COLLECTION_PERMISSION_NOT_APPROVED.name(), "정책상 건너뜀");
                }

                FetchResult fetchResult = officialSourceFetcher.fetch(sourceWatch.getUrl());
                if (!fetchResult.success()) {
                    sourceWatch.markFailed();
                    collectionRun.completeFailed(false, "FETCH_FAILED", fetchResult.failureReason());
                    return new SourceWatchCollectResponse(id, false, false, 0, "FETCH_FAILED", fetchResult.failureReason());
                }

                ExtractedText extractedText = htmlTextExtractor.extract(fetchResult.html(), sourceWatch.getUrl());
                if (!extractedText.success()) {
                    sourceWatch.markFailed();
                    collectionRun.completeFailed(true, "EXTRACT_FAILED", extractedText.failureReason());
                    return new SourceWatchCollectResponse(id, true, false, 0, "EXTRACT_FAILED", extractedText.failureReason());
                }

                String contentHash = sha256(extractedText.text());
                boolean sameAsPrevious = contentHash.equals(sourceWatch.getLastContentHash());
                PageSnapshot snapshot = pageSnapshotRepository.save(new PageSnapshot(
                        sourceWatch,
                        contentHash,
                        extractedText.text(),
                        extractedText.benefitDetailImageSources(),
                        sameAsPrevious
                ));

                int candidateCount = 0;
                if (!sameAsPrevious) {
                    candidateCount = benefitCandidateDetector.detect(sourceWatch, snapshot, collectionRun.getId()).size();
                }

                sourceWatch.markSuccess(contentHash);
            collectionRun.completeSuccess(true, sameAsPrevious, candidateCount, snapshot.getId());
            return new SourceWatchCollectResponse(id, true, sameAsPrevious, candidateCount, null, "collection completed");
            } catch (RuntimeException exception) {
                sourceWatch.markFailed();
                collectionRun.completeFailed(false, "UNKNOWN", exception.getMessage());
                return new SourceWatchCollectResponse(id, false, false, 0, "UNKNOWN", exception.getMessage());
            }
        } finally {
            lock.ifPresent(collectionRedisLock::release);
        }
    }

    private String buildRobotsErrorMessage(RobotsTxtCheckResult result) {
        StringBuilder builder = new StringBuilder(result.message());
        if (result.robotsTxtUrl() != null) {
            builder.append(" robots.txt: ").append(result.robotsTxtUrl());
        }
        if (result.matchedRule() != null) {
            builder.append(" matchedRule: ").append(result.matchedRule());
        }
        return builder.toString();
    }

    private SkippedReason getPolicySkippedReason(SourceWatch sourceWatch) {
        if (sourceWatch.getRobotsCheckStatus() == RobotsCheckStatus.DISALLOWED) {
            return SkippedReason.ROBOTS_TXT_DISALLOWED;
        }
        if (sourceWatch.getRobotsCheckStatus() == RobotsCheckStatus.FETCH_FAILED) {
            return SkippedReason.ROBOTS_TXT_FETCH_FAILED;
        }
        if (sourceWatch.getRobotsCheckStatus() == RobotsCheckStatus.PARSE_FAILED) {
            return SkippedReason.ROBOTS_TXT_PARSE_FAILED;
        }
        if (sourceWatch.getTermsCheckStatus() == TermsCheckStatus.RESTRICTION_FOUND) {
            return SkippedReason.TERMS_RESTRICTION_FOUND;
        }
        if (sourceWatch.getTermsCheckStatus() == TermsCheckStatus.NOT_CHECKED) {
            return SkippedReason.TERMS_NOT_CHECKED;
        }
        if (sourceWatch.getTermsCheckStatus() == TermsCheckStatus.NEEDS_REVIEW) {
            return SkippedReason.UNKNOWN_POLICY_NEEDS_REVIEW;
        }
        if (sourceWatch.getCollectionPermissionStatus() == CollectionPermissionStatus.LOGIN_REQUIRED) {
            return SkippedReason.LOGIN_REQUIRED_SOURCE;
        }
        if (sourceWatch.getCollectionPermissionStatus() != CollectionPermissionStatus.ALLOWED_TO_COLLECT) {
            return SkippedReason.COLLECTION_PERMISSION_NOT_APPROVED;
        }
        return null;
    }

    private RobotsCheckStatus toRobotsCheckStatus(RobotsTxtCheckResult result) {
        if (result.allowed()) {
            return "robots.txt not found".equals(result.matchedRule()) ? RobotsCheckStatus.NOT_FOUND : RobotsCheckStatus.ALLOWED;
        }
        return switch (result.failureReason()) {
            case "ROBOTS_TXT_DISALLOWED" -> RobotsCheckStatus.DISALLOWED;
            case "ROBOTS_TXT_FETCH_FAILED" -> RobotsCheckStatus.FETCH_FAILED;
            case "ROBOTS_TXT_PARSE_FAILED" -> RobotsCheckStatus.PARSE_FAILED;
            default -> RobotsCheckStatus.UNKNOWN;
        };
    }

    @Transactional
    public SourceWatchResponse updateNextFetchAt(Long id, LocalDateTime nextFetchAt) {
        SourceWatch sourceWatch = getSourceWatch(id);
        sourceWatch.updateNextFetchAt(nextFetchAt);
        return SourceWatchResponse.from(sourceWatch, getRecentRun(sourceWatch.getId()));
    }

    @Transactional
    public SourceWatchResponse markFailedAndUpdateNextFetchAt(Long id, LocalDateTime nextFetchAt) {
        SourceWatch sourceWatch = getSourceWatch(id);
        sourceWatch.markFailed();
        sourceWatch.updateNextFetchAt(nextFetchAt);
        return SourceWatchResponse.from(sourceWatch, getRecentRun(sourceWatch.getId()));
    }

    private SourceWatchCollectResponse createSkippedResponse(
            SourceWatch sourceWatch,
            CollectionTriggerType triggerType,
            String failureReason,
            String message
    ) {
        CollectionRun collectionRun = collectionRunRepository.save(new CollectionRun(sourceWatch, triggerType));
        sourceWatch.markSkipped();
        collectionRun.completeSkipped(failureReason, message);
        return new SourceWatchCollectResponse(sourceWatch.getId(), false, false, 0, failureReason, message);
    }

    private DomainRateLimitResult checkDomainRateLimit(SourceWatch sourceWatch) {
        if (minDomainIntervalSeconds <= 0) {
            return DomainRateLimitResult.notLimited();
        }
        String host = extractHost(sourceWatch.getUrl());
        if (host == null) {
            return DomainRateLimitResult.notLimited();
        }
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(minDomainIntervalSeconds);
        List<CollectionRun> recentRuns = collectionRunRepository.findRecentByStatuses(
                Set.of(CollectionRunStatus.SUCCESS, CollectionRunStatus.FAILED, CollectionRunStatus.SKIPPED),
                PageRequest.of(0, 200)
        );
        return recentRuns.stream()
                .filter(run -> run.getStartedAt() != null && !run.getStartedAt().isBefore(threshold))
                .filter(run -> host.equals(extractHost(run.getSourceWatch().getUrl())))
                .findFirst()
                .map(run -> DomainRateLimitResult.limited(run.getStartedAt()))
                .orElseGet(DomainRateLimitResult::notLimited);
    }

    private Map<Long, RecentCollectionRunSummaryResponse> getRecentRunsBySourceWatchId(List<SourceWatch> sourceWatches) {
        if (sourceWatches.isEmpty()) {
            return Map.of();
        }
        return collectionRunRepository.findLatestRunsBySourceWatchIds(
                        sourceWatches.stream().map(SourceWatch::getId).toList()
                ).stream()
                .collect(java.util.stream.Collectors.toMap(
                        run -> run.getSourceWatch().getId(),
                        RecentCollectionRunSummaryResponse::from
                ));
    }

    private RecentCollectionRunSummaryResponse getRecentRun(Long sourceWatchId) {
        return collectionRunRepository.findLatestRunsBySourceWatchIds(List.of(sourceWatchId)).stream()
                .findFirst()
                .map(RecentCollectionRunSummaryResponse::from)
                .orElse(null);
    }

    private String extractHost(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            return host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private SourceWatch getSourceWatch(Long id) {
        return sourceWatchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SourceWatch not found"));
    }

    private void validateOfficialSourceType(SourceType sourceType) {
        if (sourceType == SourceType.BLOG_REFERENCE || sourceType == SourceType.COMMUNITY_REFERENCE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Only official source URLs can be watched");
        }
    }

    private void validateManualPermissionStatus(CollectionPermissionStatus status) {
        if (status == CollectionPermissionStatus.ALLOWED_TO_COLLECT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "ALLOWED_TO_COLLECT cannot be set manually");
        }
    }

    private boolean becameBlocked(CollectionPermissionStatus before, CollectionPermissionStatus after) {
        return before != after && (after == CollectionPermissionStatus.BLOCKED_BY_ROBOTS || after == CollectionPermissionStatus.BLOCKED_BY_TERMS);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record DomainRateLimitResult(
            boolean rateLimited,
            LocalDateTime recentStartedAt
    ) {
        private static DomainRateLimitResult notLimited() {
            return new DomainRateLimitResult(false, null);
        }

        private static DomainRateLimitResult limited(LocalDateTime recentStartedAt) {
            return new DomainRateLimitResult(true, recentStartedAt);
        }
    }
}
