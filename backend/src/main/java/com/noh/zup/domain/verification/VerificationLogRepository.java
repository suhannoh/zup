package com.noh.zup.domain.verification;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    List<VerificationLog> findAllByBenefitIdOrderByVerifiedAtDesc(Long benefitId);

    List<VerificationLog> findTop20ByOrderByVerifiedAtDesc();

    List<VerificationLog> findAllByOrderByVerifiedAtDesc(Pageable pageable);
}
