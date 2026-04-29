package com.noh.zup.domain.benefit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitDetailItemRepository extends JpaRepository<BenefitDetailItem, Long> {

    List<BenefitDetailItem> findAllByBenefitIdOrderByDisplayOrderAscIdAsc(Long benefitId);

    List<BenefitDetailItem> findAllByBenefitIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long benefitId);

    long countByBenefitId(Long benefitId);
}
