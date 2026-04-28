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
  SUCCESS: "성공",
  FAILED: "실패",
  SKIPPED: "스킵",
};

export const COLLECTION_STATUS_CLASS: Record<string, string> = {
  READY: "bg-neutral-100 text-neutral-700",
  SUCCESS: "bg-green-50 text-green-700",
  FAILED: "bg-red-50 text-red-700",
  SKIPPED: "bg-amber-50 text-amber-700",
};

export const TRIGGER_TYPE_LABELS: Record<string, string> = {
  MANUAL: "수동 실행",
  SCHEDULED: "자동 실행",
};
