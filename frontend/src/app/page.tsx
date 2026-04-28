import Link from "next/link";
import { BrandCard } from "@/components/public/BrandCard";
import { CategoryPill } from "@/components/public/CategoryPill";
import { SectionHeader } from "@/components/public/SectionHeader";
import { TagPill } from "@/components/public/TagPill";
import { getBrands, getCategories, getTags } from "@/lib/api/publicApi";
import type { BrandListItem } from "@/types/brand";
import type { Category } from "@/types/category";
import type { Tag } from "@/types/tag";

export const dynamic = "force-dynamic";

async function safeList<T>(loader: () => Promise<T[]>) {
  try {
    return await loader();
  } catch {
    return [];
  }
}

export default async function HomePage() {
  const [categories, tags, brands] = await Promise.all([
    safeList<Category>(getCategories),
    safeList<Tag>(getTags),
    safeList<BrandListItem>(() => getBrands()),
  ]);

  const quickTags = tags.filter((tag) =>
    ["free", "no-app-required", "birthday-month"].includes(tag.slug)
  );
  const quickCategories = categories.filter((category) =>
    ["cafe", "movie-culture", "beauty"].includes(category.slug)
  );

  return (
    <div className="space-y-16 py-6 md:py-12">
      <section className="max-w-3xl">
        <p className="text-sm font-medium text-accent">혜택 정보 큐레이션</p>
        <h1 className="mt-4 text-4xl font-bold leading-tight md:text-6xl">Zup</h1>
        <p className="mt-4 text-2xl font-semibold leading-tight md:text-3xl">
          몰라서 못 받던 혜택, 오늘 줍자
        </p>
        <p className="mt-5 text-base leading-8 text-neutral-600 md:text-lg">
          브랜드별 생일 혜택을 공식 출처 기준으로 정리하고, 앱 필요 여부·멤버십 조건·사용 기간까지 한눈에
          확인하세요.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link className="rounded-lg bg-accent px-5 py-3 text-sm font-semibold text-white" href="/brands">
            혜택 보러가기
          </Link>
          <Link className="rounded-lg border border-border bg-white px-5 py-3 text-sm font-semibold" href="/reports/new">
            정보 제보하기
          </Link>
        </div>
        <div className="mt-6 flex flex-wrap gap-2">
          {quickTags.map((tag) => (
            <TagPill key={tag.slug} tag={tag} />
          ))}
          {quickCategories.map((category) => (
            <CategoryPill key={category.slug} category={category} />
          ))}
        </div>
      </section>

      <section>
        <SectionHeader title="카테고리 바로가기" description="관심 있는 브랜드 유형별로 생일 혜택을 확인하세요." />
        <div className="flex flex-wrap gap-3">
          {categories.map((category) => (
            <CategoryPill key={category.slug} category={category} />
          ))}
        </div>
      </section>

      <section>
        <SectionHeader
          title="대표 브랜드"
          description="현재 등록된 브랜드 중 일부를 보여줍니다. 공개 혜택은 공식 출처 검수 후 노출됩니다."
          action={
            <Link href="/brands" className="text-sm font-semibold text-accent hover:text-blue-700">
              전체 보기
            </Link>
          }
        />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {brands.slice(0, 6).map((brand) => (
            <BrandCard key={brand.slug} brand={brand} />
          ))}
        </div>
      </section>
    </div>
  );
}
