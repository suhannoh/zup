package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionRunService {
    private static final int DEFAULT_COLLECTION_RUN_LIMIT = 50;
    private static final int MAX_COLLECTION_RUN_LIMIT = 100;
    private static final int DEFAULT_SOURCE_WATCH_HISTORY_LIMIT = 10;
    private static final int MAX_SOURCE_WATCH_HISTORY_LIMIT = 20;

    private final CollectionRunRepository collectionRunRepository;
    private final SourceWatchRepository sourceWatchRepository;

    public CollectionRunService(
            CollectionRunRepository collectionRunRepository,
            SourceWatchRepository sourceWatchRepository
    ) {
        this.collectionRunRepository = collectionRunRepository;
        this.sourceWatchRepository = sourceWatchRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectionRunResponse> getRecentRuns(
            CollectionRunStatus status,
            String failureReason,
            Long sourceWatchId,
            String keyword,
            Integer limit
    ) {
        String normalizedKeyword = normalize(keyword);
        String normalizedFailureReason = normalize(failureReason);
        return collectionRunRepository.findAllDetailedOrderByStartedAtDescIdDesc().stream()
                .filter(run -> status == null || run.getStatus() == status)
                .filter(run -> normalizedFailureReason == null || normalizedFailureReason.equals(run.getFailureReason()))
                .filter(run -> sourceWatchId == null || run.getSourceWatch().getId().equals(sourceWatchId))
                .filter(run -> matchesKeyword(run, normalizedKeyword))
                .limit(normalizeCollectionRunLimit(limit))
                .map(CollectionRunResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SourceWatchCollectionRunHistoryResponse> getSourceWatchRuns(Long sourceWatchId, Integer limit) {
        if (!sourceWatchRepository.existsById(sourceWatchId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SourceWatch not found");
        }
        return collectionRunRepository.findBySourceWatchIdOrderByStartedAtDescIdDesc(
                        sourceWatchId,
                        PageRequest.of(0, normalizeLimit(limit))
                ).stream()
                .map(SourceWatchCollectionRunHistoryResponse::from)
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_SOURCE_WATCH_HISTORY_LIMIT;
        }
        return Math.min(limit, MAX_SOURCE_WATCH_HISTORY_LIMIT);
    }

    private int normalizeCollectionRunLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_COLLECTION_RUN_LIMIT;
        }
        return Math.min(limit, MAX_COLLECTION_RUN_LIMIT);
    }

    private boolean matchesKeyword(CollectionRun run, String keyword) {
        if (keyword == null) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        SourceWatch sourceWatch = run.getSourceWatch();
        return contains(sourceWatch.getTitle(), lowerKeyword)
                || contains(sourceWatch.getUrl(), lowerKeyword)
                || contains(sourceWatch.getBrand().getName(), lowerKeyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
