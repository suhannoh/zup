package com.noh.zup.domain.collection;

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
@RequestMapping("/api/v1/admin/source-watches")
public class AdminSourceWatchController {

    private final SourceWatchService sourceWatchService;

    public AdminSourceWatchController(SourceWatchService sourceWatchService) {
        this.sourceWatchService = sourceWatchService;
    }

    @GetMapping
    public ApiResponse<List<SourceWatchResponse>> getSourceWatches() {
        return ApiResponse.success(sourceWatchService.getSourceWatches(), "source watches fetched");
    }

    @PostMapping
    public ApiResponse<SourceWatchResponse> createSourceWatch(
            @Valid @RequestBody SourceWatchCreateRequest request
    ) {
        return ApiResponse.success(sourceWatchService.createSourceWatch(request), "source watch created");
    }

    @PatchMapping("/{id}")
    public ApiResponse<SourceWatchResponse> updateSourceWatch(
            @PathVariable Long id,
            @Valid @RequestBody SourceWatchUpdateRequest request
    ) {
        return ApiResponse.success(sourceWatchService.updateSourceWatch(id, request), "source watch updated");
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<SourceWatchResponse> updateActive(
            @PathVariable Long id,
            @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return ApiResponse.success(sourceWatchService.updateActive(id, request.isActive()), "source watch active updated");
    }

    @PatchMapping("/{id}/terms-check")
    public ApiResponse<SourceWatchResponse> updateTermsCheck(
            @PathVariable Long id,
            @Valid @RequestBody SourceWatchTermsCheckRequest request
    ) {
        return ApiResponse.success(sourceWatchService.updateTermsCheck(id, request), "source watch terms check updated");
    }

    @PostMapping("/{id}/collect")
    public ApiResponse<SourceWatchCollectResponse> collect(@PathVariable Long id) {
        return ApiResponse.success(sourceWatchService.collect(id), "source watch collected");
    }

    @PostMapping("/{id}/regenerate-candidates")
    public ApiResponse<SourceWatchRegenerateCandidatesResponse> regenerateCandidates(@PathVariable Long id) {
        return ApiResponse.success(
                sourceWatchService.regenerateCandidates(id),
                "candidate regeneration completed"
        );
    }
}
