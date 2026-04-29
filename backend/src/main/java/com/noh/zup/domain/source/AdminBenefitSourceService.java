package com.noh.zup.domain.source;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.benefit.BenefitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBenefitSourceService {

    private final BenefitRepository benefitRepository;
    private final BenefitSourceRepository benefitSourceRepository;

    public AdminBenefitSourceService(
            BenefitRepository benefitRepository,
            BenefitSourceRepository benefitSourceRepository
    ) {
        this.benefitRepository = benefitRepository;
        this.benefitSourceRepository = benefitSourceRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBenefitSourceResponse> getSources(Long benefitId) {
        getBenefit(benefitId);
        return benefitSourceRepository.findAllByBenefitId(benefitId).stream()
                .map(AdminBenefitSourceResponse::from)
                .toList();
    }

    @Transactional
    public AdminBenefitSourceResponse createSource(Long benefitId, AdminBenefitSourceCreateRequest request) {
        Benefit benefit = getBenefit(benefitId);
        BenefitSource source = new BenefitSource(benefit, request.sourceType(), request.sourceUrl());
        source.update(null, null, request.sourceTitle(), request.sourceCheckedAt(), request.memo());
        source.updateVerificationMetadata(
                request.officialSourceUrl(),
                request.lastVerifiedDate(),
                request.collectionMethod(),
                request.verificationSummary(),
                request.sourceNotice()
        );
        return AdminBenefitSourceResponse.from(benefitSourceRepository.save(source));
    }

    @Transactional
    public AdminBenefitSourceResponse updateSource(Long sourceId, AdminBenefitSourceUpdateRequest request) {
        BenefitSource source = getSource(sourceId);
        source.update(
                request.sourceType(),
                request.sourceUrl(),
                request.sourceTitle(),
                request.sourceCheckedAt(),
                request.memo()
        );
        source.updateVerificationMetadata(
                request.officialSourceUrl(),
                request.lastVerifiedDate(),
                request.collectionMethod(),
                request.verificationSummary(),
                request.sourceNotice()
        );
        return AdminBenefitSourceResponse.from(source);
    }

    @Transactional
    public AdminBenefitSourceResponse deleteSource(Long sourceId) {
        BenefitSource source = getSource(sourceId);
        source.deactivate();
        return AdminBenefitSourceResponse.from(source);
    }

    private Benefit getBenefit(Long id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found"));
    }

    private BenefitSource getSource(Long id) {
        return benefitSourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit source not found"));
    }
}
