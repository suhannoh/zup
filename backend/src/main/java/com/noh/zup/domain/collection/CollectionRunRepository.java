package com.noh.zup.domain.collection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    List<CollectionRun> findTop50ByOrderByStartedAtDescIdDesc();

    @Query("""
            select collectionRun
            from CollectionRun collectionRun
            join fetch collectionRun.sourceWatch sourceWatch
            join fetch sourceWatch.brand brand
            order by collectionRun.startedAt desc, collectionRun.id desc
            """)
    List<CollectionRun> findAllDetailedOrderByStartedAtDescIdDesc();

    @Query(
            value = """
                    select collectionRun
                    from CollectionRun collectionRun
                    join fetch collectionRun.sourceWatch sourceWatch
                    join fetch sourceWatch.brand brand
                    where (:status is null or collectionRun.status = :status)
                      and (:failureReason is null or collectionRun.failureReason = :failureReason)
                      and (:sourceWatchId is null or sourceWatch.id = :sourceWatchId)
                      and (
                        :keyword is null
                        or lower(sourceWatch.title) like concat('%', :keyword, '%')
                        or lower(sourceWatch.url) like concat('%', :keyword, '%')
                        or lower(brand.name) like concat('%', :keyword, '%')
                      )
                    order by collectionRun.startedAt desc, collectionRun.id desc
                    """,
            countQuery = """
                    select count(collectionRun)
                    from CollectionRun collectionRun
                    join collectionRun.sourceWatch sourceWatch
                    join sourceWatch.brand brand
                    where (:status is null or collectionRun.status = :status)
                      and (:failureReason is null or collectionRun.failureReason = :failureReason)
                      and (:sourceWatchId is null or sourceWatch.id = :sourceWatchId)
                      and (
                        :keyword is null
                        or lower(sourceWatch.title) like concat('%', :keyword, '%')
                        or lower(sourceWatch.url) like concat('%', :keyword, '%')
                        or lower(brand.name) like concat('%', :keyword, '%')
                      )
                    """
    )
    Page<CollectionRun> searchAdminCollectionRuns(
            @Param("status") CollectionRunStatus status,
            @Param("failureReason") String failureReason,
            @Param("sourceWatchId") Long sourceWatchId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<CollectionRun> findTop20BySourceWatchIdOrderByStartedAtDescIdDesc(Long sourceWatchId);

    List<CollectionRun> findBySourceWatchIdOrderByStartedAtDescIdDesc(Long sourceWatchId, Pageable pageable);

    List<CollectionRun> findBySnapshotIdIn(Collection<Long> snapshotIds);

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
