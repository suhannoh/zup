export type SourceType =
  | "OFFICIAL_HOME"
  | "OFFICIAL_APP"
  | "OFFICIAL_MEMBERSHIP"
  | "OFFICIAL_FAQ"
  | "OFFICIAL_NOTICE"
  | "OFFICIAL_SNS"
  | "CUSTOMER_CENTER"
  | "BLOG_REFERENCE"
  | "COMMUNITY_REFERENCE";

export type AdminBenefitSource = {
  id: number;
  benefitId: number;
  sourceType: SourceType;
  sourceUrl: string;
  sourceTitle: string | null;
  sourceCheckedAt: string | null;
  memo: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminBenefitSourceCreateRequest = {
  sourceType: SourceType;
  sourceUrl: string;
  sourceTitle?: string | null;
  sourceCheckedAt?: string | null;
  memo?: string | null;
};

export type AdminBenefitSourceUpdateRequest = Partial<AdminBenefitSourceCreateRequest>;
