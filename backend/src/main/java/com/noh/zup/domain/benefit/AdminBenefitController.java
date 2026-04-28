package com.noh.zup.domain.benefit;

import com.noh.zup.common.request.ActiveUpdateRequest;
import com.noh.zup.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBenefitController {

    private final AdminBenefitService adminBenefitService;

    public AdminBenefitController(AdminBenefitService adminBenefitService) {
        this.adminBenefitService = adminBenefitService;
    }

    @GetMapping("/benefits")
    public ApiResponse<List<BenefitDetailResponse>> getBenefits(
            @RequestParam(required = false) String brandSlug,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) BenefitType benefitType,
            @RequestParam(required = false) BirthdayTimingType birthdayTimingType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                adminBenefitService.getBenefits(
                        brandSlug,
                        categorySlug,
                        verificationStatus,
                        benefitType,
                        birthdayTimingType,
                        isActive,
                        keyword
                ),
                "benefits fetched"
        );
    }

    @GetMapping("/benefits/{id}")
    public ApiResponse<BenefitDetailResponse> getBenefit(@PathVariable Long id) {
        return ApiResponse.success(adminBenefitService.getBenefit(id), "benefit fetched");
    }

    @PostMapping("/benefits")
    public ApiResponse<BenefitDetailResponse> createBenefit(@Valid @RequestBody AdminBenefitCreateRequest request) {
        return ApiResponse.success(adminBenefitService.createBenefit(request), "benefit created");
    }

    @PatchMapping("/benefits/{id}")
    public ApiResponse<BenefitDetailResponse> updateBenefit(
            @PathVariable Long id,
            @Valid @RequestBody AdminBenefitUpdateRequest request
    ) {
        return ApiResponse.success(adminBenefitService.updateBenefit(id, request), "benefit updated");
    }

    @PatchMapping("/benefits/{id}/status")
    public ApiResponse<BenefitDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminBenefitStatusUpdateRequest request
    ) {
        return ApiResponse.success(adminBenefitService.updateStatus(id, request), "benefit status updated");
    }

    @PatchMapping("/benefits/{id}/active")
    public ApiResponse<BenefitDetailResponse> updateActive(
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(adminBenefitService.updateActive(id, request.isActive()), "benefit active updated");
    }

    @PostMapping("/benefits/{benefitId}/tags")
    public ApiResponse<BenefitDetailResponse> addTag(
            @PathVariable Long benefitId,
            @Valid @RequestBody AdminBenefitTagRequest request
    ) {
        return ApiResponse.success(adminBenefitService.addTag(benefitId, request.tagId()), "benefit tag added");
    }

    @DeleteMapping("/benefits/{benefitId}/tags/{tagId}")
    public ApiResponse<BenefitDetailResponse> removeTag(
            @PathVariable Long benefitId,
            @PathVariable Long tagId
    ) {
        return ApiResponse.success(adminBenefitService.removeTag(benefitId, tagId), "benefit tag removed");
    }

    @GetMapping("/review-needed")
    public ApiResponse<List<BenefitDetailResponse>> getReviewNeededBenefits() {
        return ApiResponse.success(adminBenefitService.getReviewNeededBenefits(), "review needed benefits fetched");
    }

    @GetMapping("/stale-benefits")
    public ApiResponse<List<BenefitDetailResponse>> getStaleBenefits(
            @RequestParam(defaultValue = "90") int days
    ) {
        return ApiResponse.success(adminBenefitService.getStaleBenefits(days), "stale benefits fetched");
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminBenefitService.getDashboard(), "dashboard fetched");
    }
}
