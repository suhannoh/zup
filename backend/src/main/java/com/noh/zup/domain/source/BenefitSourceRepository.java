package com.noh.zup.domain.source;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitSourceRepository extends JpaRepository<BenefitSource, Long> {

    List<BenefitSource> findAllByBenefitId(Long benefitId);

    List<BenefitSource> findAllByBenefitIdAndIsActiveTrue(Long benefitId);

    List<BenefitSource> findAllBySourceType(SourceType sourceType);

    long countByBenefitId(Long benefitId);
}
