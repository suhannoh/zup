package com.noh.zup.domain.collection;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BenefitCandidateRepository extends JpaRepository<BenefitCandidate, Long> {

    @Query("""
            select candidate
            from BenefitCandidate candidate
            join fetch candidate.brand brand
            join fetch candidate.sourceWatch sourceWatch
            join fetch candidate.snapshot snapshot
            order by candidate.id desc
            """)
    List<BenefitCandidate> findAllByOrderByIdDesc();

    @Query(
            value = """
                    select candidate
                    from BenefitCandidate candidate
                    join fetch candidate.brand brand
                    join fetch candidate.sourceWatch sourceWatch
                    join fetch candidate.snapshot snapshot
                    where (:sourceWatchId is null or sourceWatch.id = :sourceWatchId)
                      and (:status is null or candidate.status = :status)
                      and (
                        :keyword is null
                        or lower(candidate.title) like concat('%', :keyword, '%')
                        or lower(candidate.summary) like concat('%', :keyword, '%')
                        or lower(brand.name) like concat('%', :keyword, '%')
                        or lower(sourceWatch.title) like concat('%', :keyword, '%')
                      )
                      and (
                        :collectionRunId is null
                        or candidate.collectionRunId = :collectionRunId
                        or (
                          candidate.collectionRunId is null
                          and exists (
                          select 1
                          from CollectionRun run
                          where run.id = :collectionRunId
                            and run.snapshotId = snapshot.id
                          )
                        )
                      )
                    order by candidate.id desc
                    """,
            countQuery = """
                    select count(candidate)
                    from BenefitCandidate candidate
                    join candidate.brand brand
                    join candidate.sourceWatch sourceWatch
                    join candidate.snapshot snapshot
                    where (:sourceWatchId is null or sourceWatch.id = :sourceWatchId)
                      and (:status is null or candidate.status = :status)
                      and (
                        :keyword is null
                        or lower(candidate.title) like concat('%', :keyword, '%')
                        or lower(candidate.summary) like concat('%', :keyword, '%')
                        or lower(brand.name) like concat('%', :keyword, '%')
                        or lower(sourceWatch.title) like concat('%', :keyword, '%')
                      )
                      and (
                        :collectionRunId is null
                        or candidate.collectionRunId = :collectionRunId
                        or (
                          candidate.collectionRunId is null
                          and exists (
                          select 1
                          from CollectionRun run
                          where run.id = :collectionRunId
                            and run.snapshotId = snapshot.id
                          )
                        )
                      )
                    """
    )
    Page<BenefitCandidate> searchAdminCandidates(
            @Param("sourceWatchId") Long sourceWatchId,
            @Param("collectionRunId") Long collectionRunId,
            @Param("status") BenefitCandidateStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByStatus(BenefitCandidateStatus status);

    boolean existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(
            Long sourceWatchId,
            String contentHash,
            String evidenceText
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update BenefitCandidate candidate
            set candidate.needsManualReview = true,
                candidate.status = com.noh.zup.domain.collection.BenefitCandidateStatus.NEEDS_REVIEW
            where candidate.sourceWatch.id = :sourceWatchId
              and candidate.approvedBenefit is null
              and candidate.status <> com.noh.zup.domain.collection.BenefitCandidateStatus.APPROVED
            """)
    int markUnapprovedAsNeedsManualReview(Long sourceWatchId);
}
