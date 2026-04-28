import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { BenefitCard } from "@/components/public/BenefitCard";
import { CategoryPill } from "@/components/public/CategoryPill";
import { EmptyState } from "@/components/public/EmptyState";
import { SectionHeader } from "@/components/public/SectionHeader";
import { TagPill } from "@/components/public/TagPill";
import { getCategories, getTagBenefits, getTags, isApiNotFound } from "@/lib/api/publicApi";
import type { BenefitListItem } from "@/types/benefit";
import type { Category } from "@/types/category";
import type { Tag } from "@/types/tag";

export const dynamic = "force-dynamic";

type TagPageProps = {
  params: Promise<{ slug: string }>;
};

export const metadata: Metadata = {
  title: "조건별 생일 혜택 - Zup",
};

async function safeList<T>(loader: () => Promise<T[]>) {
  try {
    return await loader();
  } catch {
    return [];
  }
}

export default async function TagPage({ params }: TagPageProps) {
  const { slug } = await params;
  const [tags, categories] = await Promise.all([
    safeList<Tag>(getTags),
    safeList<Category>(getCategories),
  ]);
  const tag = tags.find((item) => item.slug === slug);

  let benefits: BenefitListItem[];
  try {
    benefits = await getTagBenefits(slug);
  } catch (error) {
    if (isApiNotFound(error)) {
      notFound();
    }
    benefits = [];
  }

  return (
    <div className="space-y-8">
      <SectionHeader
        title={`${tag?.name ?? slug} 조건의 생일 혜택`}
        description="조건 태그 기준으로 공개된 혜택을 모아봅니다. 현재는 공식 검수 완료 혜택만 노출됩니다."
      />

      {benefits.length > 0 ? (
        <div className="grid gap-4">
          {benefits.map((benefit) => (
            <BenefitCard key={benefit.id} benefit={benefit} />
          ))}
        </div>
      ) : (
        <EmptyState
          title="아직 이 조건에 해당하는 공개 혜택이 없습니다."
          description="공식 출처 확인 후 혜택이 추가될 예정입니다."
        />
      )}

      <section className="grid gap-4 rounded-xl border border-border bg-white p-5 md:grid-cols-2">
        <div>
          <h2 className="font-bold text-neutral-950">카테고리별 보기</h2>
          <div className="mt-4 flex flex-wrap gap-2">
            {categories.map((category) => (
              <CategoryPill key={category.slug} category={category} />
            ))}
          </div>
        </div>
        <div>
          <h2 className="font-bold text-neutral-950">다른 조건 보기</h2>
          <div className="mt-4 flex flex-wrap gap-2">
            {tags.slice(0, 8).map((item) => (
              <TagPill key={item.slug} tag={item} />
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
