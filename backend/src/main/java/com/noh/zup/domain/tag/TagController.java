package com.noh.zup.domain.tag;

import com.noh.zup.common.response.ApiResponse;
import com.noh.zup.domain.benefit.BenefitListResponse;
import com.noh.zup.domain.benefit.BenefitService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;
    private final BenefitService benefitService;

    public TagController(TagService tagService, BenefitService benefitService) {
        this.tagService = tagService;
        this.benefitService = benefitService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> getTags() {
        return ApiResponse.success(tagService.getTags(), "tags fetched");
    }

    @GetMapping("/{slug}/benefits")
    public ApiResponse<List<BenefitListResponse>> getBenefitsByTag(@PathVariable String slug) {
        return ApiResponse.success(benefitService.getBenefitsByTag(slug), "benefits fetched");
    }
}
