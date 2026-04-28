package com.noh.zup.domain.report;

import com.noh.zup.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public ApiResponse<List<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long benefitId
    ) {
        return ApiResponse.success(
                adminReportService.getReports(status, reportType, brandId, benefitId),
                "reports fetched"
        );
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ReportResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminReportStatusUpdateRequest request
    ) {
        return ApiResponse.success(adminReportService.updateStatus(id, request), "report status updated");
    }
}
