export type CollectionTriggerType = "MANUAL" | "SCHEDULED" | "MANUAL_REGENERATE_CANDIDATES";

export type CollectionRunStatus = "RUNNING" | "SUCCESS" | "FAILED" | "SKIPPED";

export type CollectionRun = {
  id: number;
  sourceWatchId: number;
  sourceWatchTitle: string;
  sourceWatchUrl: string;
  brandId: number;
  brandName: string;
  triggerType: CollectionTriggerType;
  status: CollectionRunStatus;
  startedAt: string;
  finishedAt: string | null;
  durationMillis: number | null;
  fetched: boolean;
  sameAsPrevious: boolean;
  candidateCount: number;
  snapshotId: number | null;
  failureReason: string | null;
  message: string;
  detailReason: string | null;
};

export type SourceWatchCollectionRunHistory = {
  id: number;
  triggerType: CollectionTriggerType;
  status: CollectionRunStatus;
  failureReason: string | null;
  fetched: boolean;
  sameAsPrevious: boolean;
  candidateCount: number;
  snapshotId: number | null;
  startedAt: string;
  finishedAt: string | null;
  message: string;
  detailReason: string | null;
};

export type CollectionRunSearchParams = {
  status?: CollectionRunStatus;
  failureReason?: string;
  sourceWatchId?: number;
  keyword?: string;
  page?: number;
  size?: number;
};
