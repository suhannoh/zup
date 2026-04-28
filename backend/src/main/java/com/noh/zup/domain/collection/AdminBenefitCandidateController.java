package com.noh.zup.domain.collection;

import com.noh.zup.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/benefit-candidates")
public class AdminBenefitCandidateController {

    private final BenefitCandidateService benefitCandidateService;

    public AdminBenefitCandidateController(BenefitCandidateService benefitCandidateService) {
        this.benefitCandidateService = benefitCandidateService;
    }

    @GetMapping
    public ApiResponse<List<BenefitCandidateResponse>> getCandidates() {
        return ApiResponse.success(benefitCandidateService.getCandidates(), "benefit candidates fetched");
    }

    @GetMapping("/{id}")
    public ApiResponse<BenefitCandidateResponse> getCandidate(@PathVariable Long id) {
        return ApiResponse.success(benefitCandidateService.getCandidate(id), "benefit candidate fetched");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<BenefitCandidateResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BenefitCandidateStatusUpdateRequest request
    ) {
        return ApiResponse.success(benefitCandidateService.updateStatus(id, request), "benefit candidate status updated");
    }
}
