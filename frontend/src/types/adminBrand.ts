export type AdminBrand = {
  id: number;
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  name: string;
  slug: string;
  description: string | null;
  officialUrl: string | null;
  membershipUrl: string | null;
  appUrl: string | null;
  brandColor: string | null;
  logoUrl: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminBrandCreateRequest = {
  categoryId: number;
  name: string;
  slug: string;
  description?: string | null;
  officialUrl?: string | null;
  membershipUrl?: string | null;
  appUrl?: string | null;
  brandColor?: string | null;
  logoUrl?: string | null;
  isActive: boolean;
};

export type AdminBrandUpdateRequest = Partial<AdminBrandCreateRequest>;

export type AdminBrandSearchParams = {
  categorySlug?: string;
  keyword?: string;
  isActive?: boolean;
};
