import type { ReportStatus, ReportType } from "@/types/report";

export const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  WRONG_INFO: "정보가 달라요",
  BENEFIT_ENDED: "혜택이 종료됐어요",
  CONDITION_CHANGED: "조건이 달라졌어요",
  NEW_BENEFIT: "새 혜택이 있어요",
  OFFICIAL_LINK_FOUND: "공식 링크를 찾았어요",
  ETC: "기타",
};

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  RECEIVED: "접수됨",
  REVIEWING: "검토 중",
  RESOLVED: "처리 완료",
  REJECTED: "반려",
};

export const REPORT_STATUS_OPTIONS: ReportStatus[] = [
  "RECEIVED",
  "REVIEWING",
  "RESOLVED",
  "REJECTED",
];

export const REPORT_TYPE_OPTIONS: ReportType[] = [
  "WRONG_INFO",
  "BENEFIT_ENDED",
  "CONDITION_CHANGED",
  "NEW_BENEFIT",
  "OFFICIAL_LINK_FOUND",
  "ETC",
];
