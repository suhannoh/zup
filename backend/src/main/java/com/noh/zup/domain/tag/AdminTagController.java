package com.noh.zup.domain.tag;

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
@RequestMapping("/api/v1/admin/tags")
public class AdminTagController {

    private final AdminTagService adminTagService;

    public AdminTagController(AdminTagService adminTagService) {
        this.adminTagService = adminTagService;
    }

    @GetMapping
    public ApiResponse<List<AdminTagResponse>> getTags() {
        return ApiResponse.success(adminTagService.getTags(), "tags fetched");
    }

    @PostMapping
    public ApiResponse<AdminTagResponse> createTag(@Valid @RequestBody AdminTagCreateRequest request) {
        return ApiResponse.success(adminTagService.createTag(request), "tag created");
    }

    @PatchMapping("/{id}")
    public ApiResponse<AdminTagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody AdminTagUpdateRequest request
    ) {
        return ApiResponse.success(adminTagService.updateTag(id, request), "tag updated");
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminTagResponse> updateActive(
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(adminTagService.updateActive(id, request.isActive()), "tag active updated");
    }
}
