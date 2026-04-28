package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionRunService {

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
    public List<CollectionRunResponse> getSourceWatchRuns(Long sourceWatchId) {
        if (!sourceWatchRepository.existsById(sourceWatchId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SourceWatch not found");
        }
        return collectionRunRepository.findTop20BySourceWatchIdOrderByStartedAtDescIdDesc(sourceWatchId).stream()
                .map(CollectionRunResponse::from)
                .toList();
    }
}
