package com.noh.zup.domain.collection;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitCandidateRepository extends JpaRepository<BenefitCandidate, Long> {

    List<BenefitCandidate> findAllByOrderByIdDesc();

    long countByStatus(BenefitCandidateStatus status);

    boolean existsBySourceWatchIdAndSnapshotContentHashAndEvidenceText(
            Long sourceWatchId,
            String contentHash,
            String evidenceText
    );
}
