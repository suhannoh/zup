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
  requiredApp: boolean;
  requiredMembership: boolean;
  requiredPurchase: boolean;
  usagePeriodDescription: string | null;
  lastVerifiedAt: string | null;
  tags: BenefitTag[];
  sources: BenefitSource[];
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
