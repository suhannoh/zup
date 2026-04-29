export type CollectionTriggerType = "MANUAL" | "SCHEDULED";

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
  endedAt: string | null;
  durationMillis: number | null;
  fetched: boolean;
  sameAsPrevious: boolean;
  candidateCount: number;
  failureReason: string | null;
  errorMessage: string | null;
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
