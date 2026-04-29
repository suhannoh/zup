package com.noh.zup.domain.collection;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    long countByStatus(BenefitCandidateStatus status);

    boolean existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(
            Long sourceWatchId,
            String contentHash,
            String evidenceText
    );
}
