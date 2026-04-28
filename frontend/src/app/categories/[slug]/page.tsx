import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { BrandCard } from "@/components/public/BrandCard";
import { EmptyState } from "@/components/public/EmptyState";
import { SectionHeader } from "@/components/public/SectionHeader";
import { TagPill } from "@/components/public/TagPill";
import { getCategories, getCategoryBrands, getTags, isApiNotFound } from "@/lib/api/publicApi";
import type { BrandListItem } from "@/types/brand";
import type { Category } from "@/types/category";
import type { Tag } from "@/types/tag";

export const dynamic = "force-dynamic";

type CategoryPageProps = {
  params: Promise<{ slug: string }>;
};

export const metadata: Metadata = {
  title: "카테고리별 생일 혜택 - Zup",
};

async function safeList<T>(loader: () => Promise<T[]>) {
  try {
    return await loader();
  } catch {
    return [];
  }
}

export default async function CategoryPage({ params }: CategoryPageProps) {
  const { slug } = await params;
  const categories = await safeList<Category>(getCategories);
  const category = categories.find((item) => item.slug === slug);

  let brands: BrandListItem[];
  try {
    brands = await getCategoryBrands(slug);
  } catch (error) {
    if (isApiNotFound(error)) {
      notFound();
    }
    brands = [];
  }

  const tags = await safeList<Tag>(getTags);
  const title = `${category?.name ?? slug} 생일 혜택 브랜드`;

  return (
    <div className="space-y-8">
      <SectionHeader
        title={title}
        description="카테고리별 브랜드를 모아보고, 각 브랜드 상세에서 공개된 생일 혜택을 확인하세요."
      />

      {brands.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {brands.map((brand) => (
            <BrandCard key={brand.slug} brand={brand} />
          ))}
        </div>
      ) : (
        <EmptyState
          title="아직 이 카테고리에 표시할 브랜드가 없습니다."
          description="공식 출처 확인 후 브랜드와 혜택이 추가될 예정입니다."
        />
      )}

      <section className="rounded-xl border border-border bg-white p-5">
        <h2 className="font-bold text-neutral-950">관련 조건으로 보기</h2>
        <div className="mt-4 flex flex-wrap gap-2">
          {tags.slice(0, 8).map((tag) => (
            <TagPill key={tag.slug} tag={tag} />
          ))}
        </div>
      </section>
    </div>
  );
}
