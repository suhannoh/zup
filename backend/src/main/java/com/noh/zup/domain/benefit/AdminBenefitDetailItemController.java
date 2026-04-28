package com.noh.zup.domain.benefit;

import com.noh.zup.common.request.ActiveUpdateRequest;
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
public class AdminBenefitDetailItemController {

    private final AdminBenefitDetailItemService adminBenefitDetailItemService;

    public AdminBenefitDetailItemController(AdminBenefitDetailItemService adminBenefitDetailItemService) {
        this.adminBenefitDetailItemService = adminBenefitDetailItemService;
    }

    @GetMapping("/benefits/{benefitId}/detail-items")
    public ApiResponse<List<BenefitDetailItemResponse>> getItems(@PathVariable Long benefitId) {
        return ApiResponse.success(adminBenefitDetailItemService.getItems(benefitId), "benefit detail items fetched");
    }

    @PostMapping("/benefits/{benefitId}/detail-items")
    public ApiResponse<BenefitDetailItemResponse> createItem(
            @PathVariable Long benefitId,
            @Valid @RequestBody BenefitDetailItemRequest request
    ) {
        return ApiResponse.success(adminBenefitDetailItemService.createItem(benefitId, request), "benefit detail item created");
    }

    @PatchMapping("/benefit-detail-items/{itemId}")
    public ApiResponse<BenefitDetailItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody BenefitDetailItemRequest request
    ) {
        return ApiResponse.success(adminBenefitDetailItemService.updateItem(itemId, request), "benefit detail item updated");
    }

    @PatchMapping("/benefit-detail-items/{itemId}/active")
    public ApiResponse<BenefitDetailItemResponse> updateActive(
            @PathVariable Long itemId,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(adminBenefitDetailItemService.updateActive(itemId, request.isActive()), "benefit detail item active updated");
    }
}
