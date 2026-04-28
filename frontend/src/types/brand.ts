import type { BenefitListItem } from "./benefit";

export type BrandListItem = {
  id: number;
  name: string;
  slug: string;
  categoryName: string;
  categorySlug: string;
  description: string | null;
  brandColor: string | null;
  logoUrl: string | null;
};

export type BrandDetail = BrandListItem & {
  officialUrl: string | null;
  membershipUrl: string | null;
  appUrl: string | null;
  benefits: BenefitListItem[];
};
