package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.common.response.PageResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionRunService {
    private static final int DEFAULT_COLLECTION_RUN_LIMIT = 50;
    private static final int MAX_COLLECTION_RUN_LIMIT = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
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
    public PageResponse<CollectionRunResponse> getRecentRuns(
            CollectionRunStatus status,
            String failureReason,
            Long sourceWatchId,
            String keyword,
            Integer page,
            Integer size
    ) {
        String normalizedKeyword = normalize(keyword);
        String normalizedFailureReason = normalize(failureReason);
        String lowerKeyword = normalizedKeyword == null ? null : normalizedKeyword.toLowerCase(Locale.ROOT);
        Page<CollectionRun> runs = collectionRunRepository.searchAdminCollectionRuns(
                status,
                normalizedFailureReason,
                sourceWatchId,
                lowerKeyword,
                PageRequest.of(normalizePage(page), normalizeSize(size))
        );
        return PageResponse.from(runs, runs.stream().map(CollectionRunResponse::from).toList());
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
