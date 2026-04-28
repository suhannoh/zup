package com.noh.zup.domain.verification;

import com.noh.zup.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminVerificationLogController {

    private final AdminVerificationLogService adminVerificationLogService;

    public AdminVerificationLogController(AdminVerificationLogService adminVerificationLogService) {
        this.adminVerificationLogService = adminVerificationLogService;
    }

    @GetMapping("/benefits/{benefitId}/verification-logs")
    public ApiResponse<List<VerificationLogResponse>> getBenefitLogs(@PathVariable Long benefitId) {
        return ApiResponse.success(
                adminVerificationLogService.getBenefitLogs(benefitId),
                "verification logs fetched"
        );
    }

    @GetMapping("/verification-logs/recent")
    public ApiResponse<List<VerificationLogResponse>> getRecentLogs(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(
                adminVerificationLogService.getRecentLogs(limit),
                "recent verification logs fetched"
        );
    }
}
