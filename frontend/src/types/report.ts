export type ReportType =
  | "WRONG_INFO"
  | "BENEFIT_ENDED"
  | "CONDITION_CHANGED"
  | "NEW_BENEFIT"
  | "OFFICIAL_LINK_FOUND"
  | "ETC";

export type ReportStatus =
  | "RECEIVED"
  | "REVIEWING"
  | "RESOLVED"
  | "REJECTED";

export type ReportCreateRequest = {
  brandId?: number | null;
  benefitId?: number | null;
  reportType: ReportType;
  content: string;
  referenceUrl?: string | null;
  email?: string | null;
};

export type AdminReport = {
  id: number;
  brandId: number | null;
  brandName: string | null;
  benefitId: number | null;
  benefitTitle: string | null;
  reportType: ReportType;
  content: string;
  referenceUrl: string | null;
  email: string | null;
  adminMemo: string | null;
  status: ReportStatus;
  createdAt: string;
  resolvedAt: string | null;
};

export type ReportResponse = AdminReport;

export type AdminDashboard = {
  brandCount: number;
  publishedBenefitCount: number;
  draftBenefitCount: number;
  needsCheckBenefitCount: number;
  expiredBenefitCount: number;
  staleBenefitCount: number;
  categoryCount: number;
  tagCount: number;
  receivedReportCount: number;
  reviewingReportCount: number;
  resolvedReportCount: number;
};
