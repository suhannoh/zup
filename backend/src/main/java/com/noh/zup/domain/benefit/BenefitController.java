package com.noh.zup.domain.benefit;

import com.noh.zup.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/benefits")
public class BenefitController {

    private final BenefitService benefitService;

    public BenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping
    public ApiResponse<List<BenefitListResponse>> getBenefits(
            @RequestParam(required = false) String brandSlug,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(required = false) BenefitType benefitType,
            @RequestParam(required = false) BirthdayTimingType birthdayTimingType,
            @RequestParam(required = false) Boolean requiredApp,
            @RequestParam(required = false) Boolean requiredMembership,
            @RequestParam(required = false) Boolean requiredPurchase
    ) {
        BenefitSearchCondition condition = new BenefitSearchCondition(
                brandSlug,
                categorySlug,
                tagSlug,
                benefitType,
                birthdayTimingType,
                requiredApp,
                requiredMembership,
                requiredPurchase
        );

        return ApiResponse.success(benefitService.getBenefits(condition), "benefits fetched");
    }

    @GetMapping("/{id}")
    public ApiResponse<BenefitDetailResponse> getBenefitDetail(@PathVariable Long id) {
        return ApiResponse.success(benefitService.getBenefitDetail(id), "benefit fetched");
    }
}
