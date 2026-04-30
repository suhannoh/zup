import type { BenefitCandidateStatus } from "@/types/benefitCandidate";

export const CANDIDATE_STATUS_LABELS: Record<BenefitCandidateStatus, string> = {
  DETECTED: "감지됨",
  NEEDS_REVIEW: "검수 대기",
  APPROVED: "승인 완료",
  REJECTED: "반려",
};

export const CANDIDATE_STATUS_CLASS: Record<BenefitCandidateStatus, string> = {
  DETECTED: "bg-blue-50 text-blue-700",
  NEEDS_REVIEW: "bg-amber-50 text-amber-700",
  APPROVED: "bg-green-50 text-green-700",
  REJECTED: "bg-red-50 text-red-700",
};

export const COLLECTION_STATUS_LABELS: Record<string, string> = {
  READY: "대기",
  RUNNING: "수집 중",
  SUCCESS: "성공",
  FAILED: "실패",
  SKIPPED: "건너뜀",
};

export const COLLECTION_STATUS_CLASS: Record<string, string> = {
  READY: "bg-neutral-100 text-neutral-700",
  RUNNING: "bg-blue-50 text-blue-700",
  SUCCESS: "bg-green-50 text-green-700",
  FAILED: "bg-red-50 text-red-700",
  SKIPPED: "bg-amber-50 text-amber-700",
};

export const TRIGGER_TYPE_LABELS: Record<string, string> = {
  MANUAL: "수동 실행",
  SCHEDULED: "자동 실행",
  MANUAL_REGENERATE_CANDIDATES: "후보 재생성",
};

export const COLLECTION_FAILURE_REASON_LABELS: Record<string, string> = {
  SOURCE_WATCH_INACTIVE: "비활성 SourceWatch",
  RATE_LIMITED_BY_DOMAIN: "도메인 최소 수집 간격 미도달",
  COLLECTION_ALREADY_RUNNING: "이미 수집 진행 중",
  SNAPSHOT_NOT_FOUND: "재생성할 스냅샷 없음",
  FETCH_FAILED: "HTML 수집 실패",
  EXTRACT_FAILED: "본문 추출 실패",
  ROBOTS_TXT_DISALLOWED: "robots.txt 차단",
  ROBOTS_TXT_FETCH_FAILED: "robots.txt 확인 실패",
  ROBOTS_TXT_PARSE_FAILED: "robots.txt 파싱 실패",
  TERMS_RESTRICTION_FOUND: "약관상 자동 수집/재배포 제한",
  TERMS_BLOCKED: "약관상 자동 수집 차단",
  TERMS_NOT_CHECKED: "약관 미확인",
  COLLECTION_PERMISSION_NOT_APPROVED: "수집 권한 미승인",
  LOGIN_REQUIRED_SOURCE: "로그인 필요 출처",
  LOGIN_REQUIRED: "로그인 필요",
  POLICY_NEEDS_REVIEW: "정책 수동 검토 필요",
  UNKNOWN_POLICY_NEEDS_REVIEW: "정책 확인 필요",
  UNKNOWN: "알 수 없는 오류",
};
