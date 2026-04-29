import type { SourceType } from "@/types/adminBenefitSource";

export type SourceWatchStatus = "READY" | "SUCCESS" | "FAILED" | "SKIPPED";
export type CollectionRunStatus = "RUNNING" | "SUCCESS" | "FAILED" | "SKIPPED";
export type CollectionTriggerType = "MANUAL" | "SCHEDULED" | "MANUAL_REGENERATE_CANDIDATES";
export type CollectionPermissionStatus =
  | "ALLOWED_TO_COLLECT"
  | "MANUAL_REVIEW_ONLY"
  | "BLOCKED_BY_ROBOTS"
  | "BLOCKED_BY_TERMS"
  | "LOGIN_REQUIRED"
  | "UNKNOWN_NEEDS_REVIEW";
export type RobotsCheckStatus = "ALLOWED" | "DISALLOWED" | "NOT_FOUND" | "FETCH_FAILED" | "PARSE_FAILED" | "UNKNOWN";
export type TermsCheckStatus = "NOT_CHECKED" | "NO_RESTRICTION_FOUND" | "RESTRICTION_FOUND" | "NEEDS_REVIEW";
export type CollectionMethod = "AUTO_COLLECTED" | "MANUAL_VERIFIED" | "MIXED" | "UNKNOWN";

export type RecentCollectionRunSummary = {
  id: number;
  triggerType: CollectionTriggerType;
  status: CollectionRunStatus;
  startedAt: string;
  fetched: boolean;
  sameAsPrevious: boolean;
  candidateCount: number;
  failureReason: string | null;
  errorMessage: string | null;
};

export type SourceWatch = {
  id: number;
  brandId: number;
  brandName: string;
  sourceType: SourceType;
  title: string;
  url: string;
  isActive: boolean;
  lastFetchedAt: string | null;
  lastContentHash: string | null;
  lastStatus: SourceWatchStatus;
  failureCount: number;
  nextFetchAt: string | null;
  loginRequired: boolean;
  collectionPermissionStatus: CollectionPermissionStatus;
  robotsCheckStatus: RobotsCheckStatus;
  termsCheckStatus: TermsCheckStatus;
  collectionMethod: CollectionMethod;
  lastPolicyCheckedAt: string | null;
  lastManualVerifiedAt: string | null;
  policyCheckNote: string | null;
  manualVerificationNote: string | null;
  recentCollectionRun: RecentCollectionRunSummary | null;
  createdAt: string;
  updatedAt: string;
};

export type SourceWatchCreateRequest = {
  brandId: number;
  sourceType: SourceType;
  title: string;
  url: string;
  isActive?: boolean | null;
  loginRequired?: boolean | null;
  robotsCheckStatus?: RobotsCheckStatus | null;
  termsCheckStatus?: TermsCheckStatus | null;
  collectionMethod?: CollectionMethod | null;
  policyCheckNote?: string | null;
  manualVerificationNote?: string | null;
};

export type SourceWatchUpdateRequest = Partial<SourceWatchCreateRequest> & {
  collectionPermissionStatus?: CollectionPermissionStatus | null;
};

export type SourceWatchCollectResponse = {
  sourceWatchId: number;
  fetched: boolean;
  sameAsPrevious: boolean;
  candidateCount: number;
  failureReason: string | null;
  message: string;
};

export type SourceWatchRegenerateCandidatesResponse = {
  sourceWatchId: number;
  collectionRunId: number;
  snapshotId: number | null;
  createdCandidateCount: number;
  skippedDuplicateCount: number;
  failureReason: string | null;
  message: string;
};
