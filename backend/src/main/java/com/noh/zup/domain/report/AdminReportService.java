package com.noh.zup.domain.report;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    private final UserReportRepository userReportRepository;

    public AdminReportService(UserReportRepository userReportRepository) {
        this.userReportRepository = userReportRepository;
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReports(
            ReportStatus status,
            ReportType reportType,
            Long brandId,
            Long benefitId
    ) {
        return userReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(report -> status == null || report.getStatus() == status)
                .filter(report -> reportType == null || report.getReportType() == reportType)
                .filter(report -> brandId == null
                        || (report.getBrand() != null && report.getBrand().getId().equals(brandId)))
                .filter(report -> benefitId == null
                        || (report.getBenefit() != null && report.getBenefit().getId().equals(benefitId)))
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional
    public ReportResponse updateStatus(Long id, AdminReportStatusUpdateRequest request) {
        UserReport report = userReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Report not found"));
        report.changeStatus(request.status());
        if (request.adminMemo() != null) {
            report.updateAdminMemo(normalize(request.adminMemo()));
        }
        return ReportResponse.from(report);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
