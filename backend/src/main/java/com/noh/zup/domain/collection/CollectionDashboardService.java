package com.noh.zup.domain.collection;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionDashboardService {

    private static final int RECENT_HOURS = 24;

    private final SourceWatchRepository sourceWatchRepository;
    private final BenefitCandidateRepository benefitCandidateRepository;
    private final CollectionRunRepository collectionRunRepository;

    public CollectionDashboardService(
            SourceWatchRepository sourceWatchRepository,
            BenefitCandidateRepository benefitCandidateRepository,
            CollectionRunRepository collectionRunRepository
    ) {
        this.sourceWatchRepository = sourceWatchRepository;
        this.benefitCandidateRepository = benefitCandidateRepository;
        this.collectionRunRepository = collectionRunRepository;
    }

    @Transactional(readOnly = true)
    public CollectionSummaryResponse getSummary() {
        LocalDateTime recentSince = LocalDateTime.now().minusHours(RECENT_HOURS);
        return new CollectionSummaryResponse(
                sourceWatchRepository.count(),
                sourceWatchRepository.countByIsActiveTrue(),
                benefitCandidateRepository.countByStatus(BenefitCandidateStatus.NEEDS_REVIEW),
                collectionRunRepository.countByStatusAndStartedAtGreaterThanEqual(
                        CollectionRunStatus.SUCCESS,
                        recentSince
                ),
                collectionRunRepository.countByStatusAndStartedAtGreaterThanEqual(
                        CollectionRunStatus.FAILED,
                        recentSince
                ),
                collectionRunRepository.countByStatusAndStartedAtGreaterThanEqual(
                        CollectionRunStatus.SKIPPED,
                        recentSince
                ),
                collectionRunRepository
                        .findTop5ByStatusAndStartedAtGreaterThanEqualOrderByStartedAtDescIdDesc(
                                CollectionRunStatus.FAILED,
                                recentSince
                        ).stream()
                        .map(CollectionFailedRunSummaryResponse::from)
                        .toList()
        );
    }
}
