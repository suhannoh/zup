import type {
  BenefitType,
  BirthdayTimingType,
  OccasionType,
  VerificationStatus,
} from "@/types/adminBenefit";

export type BenefitCandidateStatus = "DETECTED" | "NEEDS_REVIEW" | "APPROVED" | "REJECTED";

export type BenefitCandidate = {
  id: number;
  brandId: number;
  brandName: string;
  sourceWatchId: number;
  snapshotId: number;
  title: string;
  summary: string;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  requiresApp: boolean;
  requiresSignup: boolean;
  requiresMembership: boolean;
  evidenceText: string;
  benefitDetailText?: string | null;
  usageGuideText?: string | null;
  confidence: number;
  status: BenefitCandidateStatus;
  reviewMemo: string | null;
  approvedBenefitId: number | null;
  approvedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type BenefitCandidateApproveRequest = {
  title?: string | null;
  summary?: string | null;
  benefitType?: BenefitType | null;
  occasionType?: OccasionType | null;
  birthdayTimingType?: BirthdayTimingType | null;
  birthdayTimingDescription?: string | null;
  requiresApp?: boolean | null;
  requiresSignup?: boolean | null;
  requiresMembership?: boolean | null;
  minimumPurchaseDescription?: string | null;
  usageCondition?: string | null;
  adminMemo?: string | null;
};

export type BenefitCandidateApproveResponse = {
  candidateId: number;
  benefitId: number;
  verificationStatus: VerificationStatus;
  message: string;
};
