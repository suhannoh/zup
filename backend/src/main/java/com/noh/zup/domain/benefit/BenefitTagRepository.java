package com.noh.zup.domain.benefit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitTagRepository extends JpaRepository<BenefitTag, Long> {

    boolean existsByBenefitIdAndTagId(Long benefitId, Long tagId);

    List<BenefitTag> findAllByBenefitId(Long benefitId);

    List<BenefitTag> findAllByTagSlug(String tagSlug);

    Optional<BenefitTag> findByBenefitIdAndTagId(Long benefitId, Long tagId);
}
