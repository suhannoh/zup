package com.noh.zup.domain.collection;

import java.time.LocalDateTime;

public record SourceWatchCollectionRunHistoryResponse(
        Long id,
        CollectionTriggerType triggerType,
        CollectionRunStatus status,
        String failureReason,
        Boolean fetched,
        Boolean sameAsPrevious,
        Integer candidateCount,
        Long snapshotId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String message,
        String detailReason
) {
    public static SourceWatchCollectionRunHistoryResponse from(CollectionRun run) {
        return new SourceWatchCollectionRunHistoryResponse(
                run.getId(),
                run.getTriggerType(),
                run.getStatus(),
                run.getFailureReason(),
                run.getFetched(),
                run.getSameAsPrevious(),
                run.getCandidateCount(),
                run.getSnapshotId(),
                run.getStartedAt(),
                run.getEndedAt(),
                buildMessage(run),
                run.getErrorMessage()
        );
    }

    private static String buildMessage(CollectionRun run) {
        if (run.getStatus() == CollectionRunStatus.SUCCESS) {
            if (Boolean.TRUE.equals(run.getSameAsPrevious())) {
                return "동일한 본문이라 후보를 새로 만들지 않았습니다.";
            }
            return "후보 " + run.getCandidateCount() + "개 생성";
        }
        if (run.getFailureReason() == null) {
            return switch (run.getStatus()) {
                case RUNNING -> "수집이 진행 중입니다.";
                case FAILED -> "수집에 실패했습니다.";
                case SKIPPED -> "수집을 건너뛰었습니다.";
                case SUCCESS -> "수집이 완료되었습니다.";
            };
        }
        return switch (run.getFailureReason()) {
            case "SOURCE_WATCH_INACTIVE" -> "비활성 SourceWatch라 수집을 건너뛰었습니다.";
            case "RATE_LIMITED_BY_DOMAIN" -> "같은 도메인의 최근 수집 이후 최소 수집 간격이 지나지 않았습니다.";
            case "COLLECTION_ALREADY_RUNNING" -> "같은 SourceWatch 수집이 이미 진행 중입니다.";
            case "ROBOTS_TXT_DISALLOWED" -> "robots.txt 정책에 의해 수집이 차단되었습니다.";
            case "ROBOTS_TXT_FETCH_FAILED" -> "robots.txt 확인에 실패해 수집을 보류했습니다.";
            case "ROBOTS_TXT_PARSE_FAILED" -> "robots.txt 파싱에 실패해 수집을 보류했습니다.";
            case "FETCH_FAILED" -> "공식 출처 HTML 수집에 실패했습니다.";
            case "EXTRACT_FAILED" -> "본문 추출에 실패했습니다.";
            default -> "수집 처리 중 오류가 발생했습니다.";
        };
    }
}
