package com.noh.zup.domain.source;

import com.noh.zup.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBenefitSourceController {

    private final AdminBenefitSourceService adminBenefitSourceService;

    public AdminBenefitSourceController(AdminBenefitSourceService adminBenefitSourceService) {
        this.adminBenefitSourceService = adminBenefitSourceService;
    }

    @GetMapping("/benefits/{benefitId}/sources")
    public ApiResponse<List<AdminBenefitSourceResponse>> getSources(@PathVariable Long benefitId) {
        return ApiResponse.success(adminBenefitSourceService.getSources(benefitId), "benefit sources fetched");
    }

    @PostMapping("/benefits/{benefitId}/sources")
    public ApiResponse<AdminBenefitSourceResponse> createSource(
            @PathVariable Long benefitId,
            @Valid @RequestBody AdminBenefitSourceCreateRequest request
    ) {
        return ApiResponse.success(adminBenefitSourceService.createSource(benefitId, request), "benefit source created");
    }

    @PatchMapping("/sources/{sourceId}")
    public ApiResponse<AdminBenefitSourceResponse> updateSource(
            @PathVariable Long sourceId,
            @Valid @RequestBody AdminBenefitSourceUpdateRequest request
    ) {
        return ApiResponse.success(adminBenefitSourceService.updateSource(sourceId, request), "benefit source updated");
    }

    @PatchMapping("/sources/{sourceId}/delete")
    public ApiResponse<AdminBenefitSourceResponse> deleteSource(@PathVariable Long sourceId) {
        return ApiResponse.success(adminBenefitSourceService.deleteSource(sourceId), "benefit source deleted");
    }
}
