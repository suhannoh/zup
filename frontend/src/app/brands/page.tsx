import type { Metadata } from "next";
import Link from "next/link";
import { BrandCard } from "@/components/public/BrandCard";
import { CategoryPill } from "@/components/public/CategoryPill";
import { EmptyState } from "@/components/public/EmptyState";
import { SectionHeader } from "@/components/public/SectionHeader";
import { getBrands, getCategories } from "@/lib/api/publicApi";
import type { BrandListItem } from "@/types/brand";
import type { Category } from "@/types/category";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "브랜드별 생일 혜택 - Zup",
  description: "카페, 외식, 영화관, 뷰티 브랜드의 생일 혜택을 한눈에 확인하세요.",
};

type BrandsPageProps = {
  searchParams: Promise<{
    categorySlug?: string;
    keyword?: string;
  }>;
};

async function safeList<T>(loader: () => Promise<T[]>) {
  try {
    return await loader();
  } catch {
    return [];
  }
}

export default async function BrandsPage({ searchParams }: BrandsPageProps) {
  const { categorySlug, keyword } = await searchParams;
  const [categories, brands] = await Promise.all([
    safeList<Category>(getCategories),
    safeList<BrandListItem>(() => getBrands({ categorySlug, keyword })),
  ]);

  return (
    <div className="space-y-8">
      <SectionHeader
        title="브랜드별 생일 혜택"
        description="브랜드명, 카테고리 기준으로 현재 공개 가능한 혜택 정보를 찾아보세요."
      />

      <form className="rounded-xl border border-border bg-white p-4" action="/brands">
        <div className="grid gap-3 md:grid-cols-[1fr_220px_auto]">
          <input
            className="h-11 rounded-lg border border-border px-4 text-sm outline-none focus:border-accent"
            name="keyword"
            defaultValue={keyword ?? ""}
            placeholder="브랜드명 검색"
          />
          <select
            className="h-11 rounded-lg border border-border bg-white px-4 text-sm outline-none focus:border-accent"
            name="categorySlug"
            defaultValue={categorySlug ?? ""}
          >
            <option value="">전체 카테고리</option>
            {categories.map((category) => (
              <option key={category.slug} value={category.slug}>
                {category.name}
              </option>
            ))}
          </select>
          <button className="h-11 rounded-lg bg-accent px-5 text-sm font-semibold text-white" type="submit">
            검색
          </button>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Link
            href="/brands"
            className="inline-flex h-9 items-center rounded-full border border-border bg-white px-3 text-sm font-medium"
          >
            전체
          </Link>
          {categories.map((category) => (
            <CategoryPill key={category.slug} category={category} />
          ))}
        </div>
      </form>

      {brands.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {brands.map((brand) => (
            <BrandCard key={brand.slug} brand={brand} />
          ))}
        </div>
      ) : (
        <EmptyState title="조건에 맞는 브랜드가 없습니다." description="검색어를 줄이거나 다른 카테고리를 선택해 보세요." />
      )}
    </div>
  );
}
