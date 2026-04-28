package com.noh.zup.domain.collection;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenefitCandidateService {

    private final BenefitCandidateRepository benefitCandidateRepository;

    public BenefitCandidateService(BenefitCandidateRepository benefitCandidateRepository) {
        this.benefitCandidateRepository = benefitCandidateRepository;
    }

    @Transactional(readOnly = true)
    public List<BenefitCandidateResponse> getCandidates() {
        return benefitCandidateRepository.findAllByOrderByIdDesc().stream()
                .map(BenefitCandidateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BenefitCandidateResponse getCandidate(Long id) {
        return BenefitCandidateResponse.from(getCandidateEntity(id));
    }

    @Transactional
    public BenefitCandidateResponse updateStatus(Long id, BenefitCandidateStatusUpdateRequest request) {
        if (request.status() == BenefitCandidateStatus.DETECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "DETECTED cannot be set manually");
        }
        BenefitCandidate candidate = getCandidateEntity(id);
        candidate.updateStatus(request.status(), request.reviewMemo());
        return BenefitCandidateResponse.from(candidate);
    }

    private BenefitCandidate getCandidateEntity(Long id) {
        return benefitCandidateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "BenefitCandidate not found"));
    }
}
