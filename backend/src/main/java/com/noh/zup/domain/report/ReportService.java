package com.noh.zup.domain.report;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.domain.benefit.Benefit;
import com.noh.zup.domain.benefit.BenefitRepository;
import com.noh.zup.domain.benefit.VerificationStatus;
import com.noh.zup.domain.brand.Brand;
import com.noh.zup.domain.brand.BrandRepository;
import com.noh.zup.domain.verification.VerificationLog;
import com.noh.zup.domain.verification.VerificationLogRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReportService {

    private static final Set<ReportType> NEEDS_CHECK_REPORT_TYPES = EnumSet.of(
            ReportType.WRONG_INFO,
            ReportType.BENEFIT_ENDED,
            ReportType.CONDITION_CHANGED,
            ReportType.OFFICIAL_LINK_FOUND
    );

    private final UserReportRepository userReportRepository;
    private final BrandRepository brandRepository;
    private final BenefitRepository benefitRepository;
    private final VerificationLogRepository verificationLogRepository;

    public ReportService(
            UserReportRepository userReportRepository,
            BrandRepository brandRepository,
            BenefitRepository benefitRepository,
            VerificationLogRepository verificationLogRepository
    ) {
        this.userReportRepository = userReportRepository;
        this.brandRepository = brandRepository;
        this.benefitRepository = benefitRepository;
        this.verificationLogRepository = verificationLogRepository;
    }

    @Transactional
    public ReportResponse createReport(ReportCreateRequest request) {
        Brand brand = request.brandId() == null ? null : getBrand(request.brandId());
        Benefit benefit = request.benefitId() == null ? null : getBenefit(request.benefitId());

        if (brand != null && benefit != null && !benefit.getBrand().getId().equals(brand.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Benefit does not belong to brand");
        }

        if (brand == null && benefit != null) {
            brand = benefit.getBrand();
        }

        UserReport report = new UserReport(
                brand,
                benefit,
                request.reportType(),
                request.content().trim(),
                normalize(request.referenceUrl()),
                normalize(request.email())
        );
        transitionBenefitToNeedsCheckIfRequired(benefit, request.reportType());
        return ReportResponse.from(userReportRepository.save(report));
    }

    private void transitionBenefitToNeedsCheckIfRequired(Benefit benefit, ReportType reportType) {
        if (benefit == null || !NEEDS_CHECK_REPORT_TYPES.contains(reportType)) {
            return;
        }
        VerificationStatus beforeStatus = benefit.getVerificationStatus();
        if (beforeStatus == VerificationStatus.NEEDS_CHECK) {
            return;
        }
        benefit.changeStatus(VerificationStatus.NEEDS_CHECK, benefit.getLastVerifiedAt());
        verificationLogRepository.save(new VerificationLog(
                benefit,
                beforeStatus,
                VerificationStatus.NEEDS_CHECK,
                "\uC0AC\uC6A9\uC790 \uC81C\uBCF4\uB85C \uAC80\uC218 \uD544\uC694 \uC804\uD658: " + reportType,
                null,
                LocalDateTime.now()
        ));
    }

    private Brand getBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Brand not found"));
    }

    private Benefit getBenefit(Long id) {
        return benefitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Benefit not found"));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
