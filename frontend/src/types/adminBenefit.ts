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

export type AdminBenefitDetail = {
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
  detailItems?: AdminBenefitDetailItem[];
};

export type AdminBenefitSummary = {
  id: number;
  brandId: number;
  brandName: string;
  brandSlug: string;
  title: string;
  summary: string;
  benefitType: BenefitType;
  applicableTiming: OccasionType;
  verificationStatus: VerificationStatus;
  isActive: boolean;
  lastVerifiedAt: string | null;
  detailItemCount: number;
  sourceCount: number;
  tagCount: number;
  createdAt: string;
  updatedAt: string;
};

export type AdminBenefit = AdminBenefitDetail;

export type AdminBenefitDetailItem = {
  id: number;
  brandName: string | null;
  title: string;
  description: string | null;
  conditionText: string | null;
  imageUrl: string | null;
  displayOrder: number;
  isActive: boolean;
};

export type AdminBenefitDetailItemRequest = {
  brandName?: string | null;
  title?: string | null;
  description?: string | null;
  conditionText?: string | null;
  imageUrl?: string | null;
  displayOrder?: number | null;
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
  officialSourceUrl: string | null;
  lastVerifiedDate: string | null;
  collectionMethod: string | null;
  verificationSummary: string | null;
  sourceNotice: string | null;
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

export type AdminManualBenefitDetailItemRequest = {
  brandName?: string | null;
  title: string;
  description?: string | null;
  conditionText?: string | null;
  imageUrl?: string | null;
  displayOrder?: number | null;
  isActive?: boolean | null;
};

export type AdminManualBenefitSourceRequest = {
  sourceType: string;
  sourceUrl: string;
  sourceTitle?: string | null;
  sourceCheckedAt: string;
  memo?: string | null;
  collectionMethod?: "AUTO_COLLECTED" | "MANUAL_VERIFIED" | "MIXED" | "UNKNOWN" | null;
  verificationSummary?: string | null;
  sourceNotice?: string | null;
};

export type AdminManualBenefitCreateRequest = AdminBenefitCreateRequest & {
  requiredSignup?: boolean | null;
  detailItems?: AdminManualBenefitDetailItemRequest[];
  sources: AdminManualBenefitSourceRequest[];
};

export type AdminBenefitSearchParams = {
  keyword?: string;
  brandSlug?: string;
  categorySlug?: string;
  verificationStatus?: VerificationStatus;
  benefitType?: BenefitType;
  birthdayTimingType?: BirthdayTimingType;
  isActive?: boolean;
  limit?: number;
};
