import type { SourceType } from "@/types/adminBenefitSource";

export type SourceWatchStatus = "READY" | "SUCCESS" | "FAILED" | "SKIPPED";

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
  createdAt: string;
  updatedAt: string;
};

export type SourceWatchCreateRequest = {
  brandId: number;
  sourceType: SourceType;
  title: string;
  url: string;
  isActive?: boolean | null;
};

export type SourceWatchUpdateRequest = Partial<SourceWatchCreateRequest>;

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
  snapshotId: number;
  createdCandidateCount: number;
  skippedDuplicateCount: number;
  message: string;
};
