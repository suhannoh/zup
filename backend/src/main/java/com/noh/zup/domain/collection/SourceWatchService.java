package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.collection.candidate.BenefitCandidateDetector;
import com.noh.zup.domain.collection.extract.ExtractedText;
import com.noh.zup.domain.collection.extract.HtmlTextExtractor;
import com.noh.zup.domain.collection.fetch.FetchResult;
import com.noh.zup.domain.collection.fetch.OfficialSourceFetcher;
import com.noh.zup.domain.source.SourceType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceWatchService {

    private final SourceWatchRepository sourceWatchRepository;
    private final PageSnapshotRepository pageSnapshotRepository;
    private final BrandRepository brandRepository;
    private final OfficialSourceFetcher officialSourceFetcher;
    private final HtmlTextExtractor htmlTextExtractor;
    private final BenefitCandidateDetector benefitCandidateDetector;

    public SourceWatchService(
            SourceWatchRepository sourceWatchRepository,
            PageSnapshotRepository pageSnapshotRepository,
            BrandRepository brandRepository,
            OfficialSourceFetcher officialSourceFetcher,
            HtmlTextExtractor htmlTextExtractor,
            BenefitCandidateDetector benefitCandidateDetector
    ) {
        this.sourceWatchRepository = sourceWatchRepository;
        this.pageSnapshotRepository = pageSnapshotRepository;
        this.brandRepository = brandRepository;
        this.officialSourceFetcher = officialSourceFetcher;
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
        SourceWatch sourceWatch = getSourceWatch(id);
        if (!Boolean.TRUE.equals(sourceWatch.getIsActive())) {
            sourceWatch.markSkipped();
            return new SourceWatchCollectResponse(id, false, false, 0, "source watch is inactive");
        }

        FetchResult fetchResult = officialSourceFetcher.fetch(sourceWatch.getUrl());
        if (!fetchResult.success()) {
            sourceWatch.markFailed();
            return new SourceWatchCollectResponse(id, false, false, 0, fetchResult.failureReason());
        }

        ExtractedText extractedText = htmlTextExtractor.extract(fetchResult.html());
        if (!extractedText.success()) {
            sourceWatch.markFailed();
            return new SourceWatchCollectResponse(id, true, false, 0, extractedText.failureReason());
        }

        String contentHash = sha256(extractedText.text());
        boolean sameAsPrevious = contentHash.equals(sourceWatch.getLastContentHash());
        PageSnapshot snapshot = pageSnapshotRepository.save(new PageSnapshot(
                sourceWatch,
                contentHash,
                extractedText.text(),
                sameAsPrevious
        ));

        int candidateCount = 0;
        if (!sameAsPrevious) {
            candidateCount = benefitCandidateDetector.detect(sourceWatch, snapshot).size();
        }

        sourceWatch.markSuccess(contentHash);
        return new SourceWatchCollectResponse(id, true, sameAsPrevious, candidateCount, "collection completed");
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
