import type { VerificationStatus } from "@/types/adminBenefit";

export type VerificationLog = {
  id: number;
  benefitId: number;
  beforeStatus: VerificationStatus;
  afterStatus: VerificationStatus;
  memo: string | null;
  adminEmail: string | null;
  verifiedAt: string;
};
