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
import com.noh.zup.domain.source.SourceType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceWatchService {

    private final SourceWatchRepository sourceWatchRepository;
    private final PageSnapshotRepository pageSnapshotRepository;
    private final BrandRepository brandRepository;
    private final CollectionRunRepository collectionRunRepository;
    private final OfficialSourceFetcher officialSourceFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final HtmlTextExtractor htmlTextExtractor;
    private final BenefitCandidateDetector benefitCandidateDetector;

    public SourceWatchService(
            SourceWatchRepository sourceWatchRepository,
            PageSnapshotRepository pageSnapshotRepository,
            BrandRepository brandRepository,
            CollectionRunRepository collectionRunRepository,
            OfficialSourceFetcher officialSourceFetcher,
            RobotsTxtChecker robotsTxtChecker,
            HtmlTextExtractor htmlTextExtractor,
            BenefitCandidateDetector benefitCandidateDetector
    ) {
        this.sourceWatchRepository = sourceWatchRepository;
        this.pageSnapshotRepository = pageSnapshotRepository;
        this.brandRepository = brandRepository;
        this.collectionRunRepository = collectionRunRepository;
        this.officialSourceFetcher = officialSourceFetcher;
        this.robotsTxtChecker = robotsTxtChecker;
        this.htmlTextExtractor = htmlTextExtractor;
        this.benefitCandidateDetector = benefitCandidateDetector;
    }

    @Transactional(readOnly = true)
    public List<SourceWatchResponse> getSourceWatches() {
        return sourceWatchRepository.findAllByOrderByIdDesc().stream()
                .map(SourceWatchResponse::from)
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
        return SourceWatchResponse.from(sourceWatchRepository.save(sourceWatch));
    }

    @Transactional
    public SourceWatchResponse updateSourceWatch(Long id, SourceWatchUpdateRequest request) {
        SourceWatch sourceWatch = getSourceWatch(id);
        if (request.sourceType() != null) {
            validateOfficialSourceType(request.sourceType());
        }
        sourceWatch.update(request.sourceType(), request.title(), request.url(), request.isActive());
        return SourceWatchResponse.from(sourceWatch);
    }

    @Transactional
    public SourceWatchResponse updateActive(Long id, Boolean isActive) {
        SourceWatch sourceWatch = getSourceWatch(id);
        sourceWatch.changeActive(isActive);
        return SourceWatchResponse.from(sourceWatch);
    }

    @Transactional
    public SourceWatchCollectResponse collect(Long id) {
        return collect(id, CollectionTriggerType.MANUAL);
    }

    @Transactional
    public SourceWatchRegenerateCandidatesResponse regenerateCandidates(Long id) {
        SourceWatch sourceWatch = getSourceWatch(id);
        PageSnapshot snapshot = pageSnapshotRepository.findTopBySourceWatchIdOrderByFetchedAtDescIdDesc(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Latest PageSnapshot not found"));

        BenefitCandidateDetectionResult result = benefitCandidateDetector.detectWithResult(sourceWatch, snapshot);
        return new SourceWatchRegenerateCandidatesResponse(
                sourceWatch.getId(),
                snapshot.getId(),
                result.createdCandidateCount(),
                result.skippedDuplicateCount(),
                "candidate regeneration completed"
        );
    }

    @Transactional
    public SourceWatchCollectResponse collect(Long id, CollectionTriggerType triggerType) {
        SourceWatch sourceWatch = getSourceWatch(id);
        CollectionRun collectionRun = collectionRunRepository.save(new CollectionRun(sourceWatch, triggerType));
        if (!Boolean.TRUE.equals(sourceWatch.getIsActive())) {
            sourceWatch.markSkipped();
            collectionRun.completeSkipped("SOURCE_WATCH_INACTIVE", "source watch is inactive");
            return new SourceWatchCollectResponse(id, false, false, 0, "SOURCE_WATCH_INACTIVE", "source watch is inactive");
        }

        try {
            RobotsTxtCheckResult robotsTxtCheckResult = robotsTxtChecker.check(sourceWatch.getUrl());
            if (!robotsTxtCheckResult.allowed()) {
                sourceWatch.markSkipped();
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
                candidateCount = benefitCandidateDetector.detect(sourceWatch, snapshot).size();
            }

            sourceWatch.markSuccess(contentHash);
            collectionRun.completeSuccess(true, sameAsPrevious, candidateCount);
            return new SourceWatchCollectResponse(id, true, sameAsPrevious, candidateCount, null, "collection completed");
        } catch (RuntimeException exception) {
            sourceWatch.markFailed();
            collectionRun.completeFailed(false, "UNKNOWN", exception.getMessage());
            return new SourceWatchCollectResponse(id, false, false, 0, "UNKNOWN", exception.getMessage());
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

    @Transactional
    public SourceWatchResponse updateNextFetchAt(Long id, LocalDateTime nextFetchAt) {
        SourceWatch sourceWatch = getSourceWatch(id);
        sourceWatch.updateNextFetchAt(nextFetchAt);
        return SourceWatchResponse.from(sourceWatch);
    }

    @Transactional
    public SourceWatchResponse markFailedAndUpdateNextFetchAt(Long id, LocalDateTime nextFetchAt) {
        SourceWatch sourceWatch = getSourceWatch(id);
        sourceWatch.markFailed();
        sourceWatch.updateNextFetchAt(nextFetchAt);
        return SourceWatchResponse.from(sourceWatch);
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

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
