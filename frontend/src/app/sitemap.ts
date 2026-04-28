import type { MetadataRoute } from "next";
import { getBrands, getCategories, getTags } from "@/lib/api/publicApi";
import { absoluteUrl } from "@/lib/seo";
import type { BrandListItem } from "@/types/brand";
import type { Category } from "@/types/category";
import type { Tag } from "@/types/tag";

async function safeList<T>(loader: () => Promise<T[]>) {
  try {
    return await loader();
  } catch {
    return [] as T[];
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const now = new Date();
  const [categories, tags, brands] = await Promise.all([
    safeList<Category>(getCategories),
    safeList<Tag>(getTags),
    safeList<BrandListItem>(() => getBrands()),
  ]);

  return [
    {
      url: absoluteUrl("/"),
      lastModified: now,
      changeFrequency: "daily",
      priority: 1,
    },
    {
      url: absoluteUrl("/brands"),
      lastModified: now,
      changeFrequency: "daily",
      priority: 0.9,
    },
    ...brands.map((brand) => ({
      url: absoluteUrl(`/brands/${brand.slug}`),
      lastModified: now,
      changeFrequency: "weekly" as const,
      priority: 0.8,
    })),
    ...categories.map((category) => ({
      url: absoluteUrl(`/categories/${category.slug}`),
      lastModified: now,
      changeFrequency: "weekly" as const,
      priority: 0.7,
    })),
    ...tags.map((tag) => ({
      url: absoluteUrl(`/tags/${tag.slug}`),
      lastModified: now,
      changeFrequency: "weekly" as const,
      priority: 0.6,
    })),
    {
      url: absoluteUrl("/reports/new"),
      lastModified: now,
      changeFrequency: "monthly",
      priority: 0.3,
    },
  ];
}
