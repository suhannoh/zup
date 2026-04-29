package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionRunService {
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
    public List<CollectionRunResponse> getRecentRuns() {
        return collectionRunRepository.findTop50ByOrderByStartedAtDescIdDesc().stream()
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
}
