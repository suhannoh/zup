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
