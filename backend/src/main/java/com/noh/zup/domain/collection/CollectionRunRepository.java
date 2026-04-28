package com.noh.zup.domain.collection;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    List<CollectionRun> findTop50ByOrderByStartedAtDescIdDesc();

    List<CollectionRun> findTop20BySourceWatchIdOrderByStartedAtDescIdDesc(Long sourceWatchId);

    long countByStatusAndStartedAtGreaterThanEqual(CollectionRunStatus status, LocalDateTime startedAt);

    List<CollectionRun> findTop5ByStatusAndStartedAtGreaterThanEqualOrderByStartedAtDescIdDesc(
            CollectionRunStatus status,
            LocalDateTime startedAt
    );
}
