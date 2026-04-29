package com.noh.zup.domain.collection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    List<CollectionRun> findTop50ByOrderByStartedAtDescIdDesc();

    List<CollectionRun> findTop20BySourceWatchIdOrderByStartedAtDescIdDesc(Long sourceWatchId);

    List<CollectionRun> findBySourceWatchIdOrderByStartedAtDescIdDesc(Long sourceWatchId, Pageable pageable);

    long countByStatusAndStartedAtGreaterThanEqual(CollectionRunStatus status, LocalDateTime startedAt);

    List<CollectionRun> findTop5ByStatusAndStartedAtGreaterThanEqualOrderByStartedAtDescIdDesc(
            CollectionRunStatus status,
            LocalDateTime startedAt
    );

    @Query("""
            select collectionRun
            from CollectionRun collectionRun
            join fetch collectionRun.sourceWatch sourceWatch
            join fetch sourceWatch.brand brand
            where collectionRun.status in :statuses
            order by collectionRun.startedAt desc, collectionRun.id desc
            """)
    List<CollectionRun> findRecentByStatuses(
            @Param("statuses") Collection<CollectionRunStatus> statuses,
            Pageable pageable
    );

    @Query("""
            select collectionRun
            from CollectionRun collectionRun
            join fetch collectionRun.sourceWatch sourceWatch
            join fetch sourceWatch.brand brand
            where sourceWatch.id in :sourceWatchIds
              and not exists (
                    select 1
                    from CollectionRun newer
                    where newer.sourceWatch = collectionRun.sourceWatch
                      and (newer.startedAt > collectionRun.startedAt
                           or (newer.startedAt = collectionRun.startedAt and newer.id > collectionRun.id))
              )
            order by collectionRun.startedAt desc, collectionRun.id desc
            """)
    List<CollectionRun> findLatestRunsBySourceWatchIds(@Param("sourceWatchIds") Collection<Long> sourceWatchIds);
}
