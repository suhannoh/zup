package com.noh.zup.domain.collection;

import com.noh.zup.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCollectionRunController {

    private final CollectionRunService collectionRunService;

    public AdminCollectionRunController(CollectionRunService collectionRunService) {
        this.collectionRunService = collectionRunService;
    }

    @GetMapping("/collection-runs")
    public ApiResponse<List<CollectionRunResponse>> getRecentRuns() {
        return ApiResponse.success(collectionRunService.getRecentRuns(), "collection runs fetched");
    }

    @GetMapping("/source-watches/{id}/collection-runs")
    public ApiResponse<List<SourceWatchCollectionRunHistoryResponse>> getSourceWatchRuns(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(
                collectionRunService.getSourceWatchRuns(id, limit),
                "source watch collection runs fetched"
        );
    }
}
