export type BenefitType =
  | "FREE_ITEM"
  | "DISCOUNT"
  | "COUPON"
  | "POINT"
  | "GIFT"
  | "UPGRADE"
  | "ETC";

export type OccasionType =
  | "BIRTHDAY"
  | "NEW_SIGNUP"
  | "APP_INSTALL"
  | "FIRST_PURCHASE"
  | "ANNIVERSARY"
  | "SEASON";

export type BirthdayTimingType =
  | "BIRTHDAY_ONLY"
  | "BIRTHDAY_MONTH"
  | "BEFORE_AFTER_DAYS"
  | "ISSUED_BEFORE_BIRTHDAY"
  | "UNKNOWN";

export type VerificationStatus =
  | "DRAFT"
  | "NEEDS_CHECK"
  | "VERIFIED"
  | "PUBLISHED"
  | "EXPIRED"
  | "HIDDEN";

export type AdminBenefit = {
  id: number;
  brandId: number;
  brandName: string;
  brandSlug: string;
  categoryName: string;
  categorySlug: string;
  title: string;
  summary: string;
  detail: string | null;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  conditionSummary: string | null;
  requiredApp: boolean;
  requiredMembership: boolean;
  requiredPurchase: boolean;
  membershipGrade: string | null;
  usagePeriodDescription: string | null;
  availableFrom: string | null;
  availableTo: string | null;
  caution: string | null;
  verificationStatus: VerificationStatus;
  lastVerifiedAt: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  tags: AdminBenefitTag[];
  sources: AdminBenefitSourceSummary[];
};

export type AdminBenefitTag = {
  name: string;
  slug: string;
};

export type AdminBenefitSourceSummary = {
  sourceType: string;
  sourceUrl: string;
  sourceTitle: string | null;
  sourceCheckedAt: string | null;
};

export type AdminBenefitCreateRequest = {
  brandId: number;
  title: string;
  summary: string;
  detail?: string | null;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  conditionSummary?: string | null;
  requiredApp: boolean;
  requiredMembership: boolean;
  requiredPurchase: boolean;
  membershipGrade?: string | null;
  usagePeriodDescription?: string | null;
  availableFrom?: string | null;
  availableTo?: string | null;
  caution?: string | null;
  verificationStatus: VerificationStatus;
  lastVerifiedAt?: string | null;
  isActive: boolean;
};

export type AdminBenefitUpdateRequest = Partial<AdminBenefitCreateRequest>;

export type AdminBenefitSearchParams = {
  keyword?: string;
  brandSlug?: string;
  categorySlug?: string;
  verificationStatus?: VerificationStatus;
  benefitType?: BenefitType;
  birthdayTimingType?: BirthdayTimingType;
  isActive?: boolean;
};
