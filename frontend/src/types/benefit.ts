export type BenefitTag = {
  name: string;
  slug: string;
};

export type BenefitSource = {
  sourceType: string;
  sourceUrl: string;
  sourceTitle: string | null;
  sourceCheckedAt: string | null;
};

export type BenefitDetailItem = {
  id: number;
  brandName: string | null;
  title: string;
  description: string | null;
  conditionText: string | null;
  imageUrl: string | null;
  displayOrder: number;
  isActive: boolean;
};

export type BenefitListItem = {
  id: number;
  brandId: number;
  brandName: string;
  brandSlug: string;
  categoryName: string;
  categorySlug: string;
  title: string;
  summary: string;
  benefitType: string;
  occasionType: string;
  birthdayTimingType: string;
  conditionSummary: string | null;
  requiredApp: boolean;
  requiredMembership: boolean;
  requiredPurchase: boolean;
  membershipGrade: string | null;
  usagePeriodDescription: string | null;
  availableFrom: string | null;
  availableTo: string | null;
  caution: string | null;
  lastVerifiedAt: string | null;
  tags: BenefitTag[];
  sources: BenefitSource[];
  detailItems?: BenefitDetailItem[];
};

export type BenefitSearchParams = {
  brandSlug?: string;
  categorySlug?: string;
  tagSlug?: string;
  benefitType?: string;
  birthdayTimingType?: string;
  requiredApp?: boolean;
  requiredMembership?: boolean;
  requiredPurchase?: boolean;
};
