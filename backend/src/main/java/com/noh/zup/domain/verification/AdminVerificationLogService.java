package com.noh.zup.domain.verification;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.benefit.BenefitRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminVerificationLogService {

    private final VerificationLogRepository verificationLogRepository;
    private final BenefitRepository benefitRepository;

    public AdminVerificationLogService(
            VerificationLogRepository verificationLogRepository,
            BenefitRepository benefitRepository
    ) {
        this.verificationLogRepository = verificationLogRepository;
        this.benefitRepository = benefitRepository;
    }

    @Transactional(readOnly = true)
    public List<VerificationLogResponse> getBenefitLogs(Long benefitId) {
        if (!benefitRepository.existsById(benefitId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found");
        }

        return verificationLogRepository.findAllByBenefitIdOrderByVerifiedAtDesc(benefitId).stream()
                .map(VerificationLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificationLogResponse> getRecentLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return verificationLogRepository.findAllByOrderByVerifiedAtDesc(PageRequest.of(0, safeLimit)).stream()
                .map(VerificationLogResponse::from)
                .toList();
    }
}
