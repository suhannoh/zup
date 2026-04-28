import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { BenefitCard } from "@/components/public/BenefitCard";
import { EmptyState } from "@/components/public/EmptyState";
import { SectionHeader } from "@/components/public/SectionHeader";
import { getBrandDetail, isApiNotFound } from "@/lib/api/publicApi";
import type { BrandDetail } from "@/types/brand";

export const dynamic = "force-dynamic";

type BrandDetailPageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: BrandDetailPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const brand = await getBrandDetail(slug);
    const title = `${brand.name} 생일 혜택 - Zup`;
    const description = `${brand.name} 생일 혜택의 앱 필요 여부, 멤버십 조건, 사용 기간, 공식 출처를 확인하세요.`;

    return {
      title,
      description,
      alternates: {
        canonical: `/brands/${slug}`,
      },
      openGraph: {
        title,
        description,
        url: `/brands/${slug}`,
      },
      twitter: {
        title,
        description,
      },
    };
  } catch {
    return {
      title: "브랜드 생일 혜택 - Zup",
      description: "브랜드별 생일 혜택의 앱 필요 여부, 멤버십 조건, 사용 기간을 확인하세요.",
      alternates: {
        canonical: `/brands/${slug}`,
      },
    };
  }
}

export default async function BrandDetailPage({ params }: BrandDetailPageProps) {
  const { slug } = await params;

  let brand: BrandDetail;
  try {
    brand = await getBrandDetail(slug);
  } catch (error) {
    if (isApiNotFound(error)) {
      notFound();
    }
    return (
      <EmptyState
        title="브랜드 정보를 불러오지 못했습니다."
        description="잠시 후 다시 시도하거나 백엔드 API 실행 상태를 확인해 주세요."
      />
    );
  }

  return (
    <div className="space-y-10">
      <section className="rounded-xl border border-border bg-white p-6">
        <Link href={`/categories/${brand.categorySlug}`} className="text-sm font-semibold text-accent">
          {brand.categoryName}
        </Link>
        <h1 className="mt-2 text-3xl font-bold text-neutral-950">{brand.name}</h1>
        <p className="mt-4 max-w-2xl text-sm leading-7 text-neutral-600">
          {brand.description ?? "공식 출처 기준으로 공개 가능한 생일 혜택 정보를 확인 중입니다."}
        </p>
        <div className="mt-5 flex flex-wrap gap-2 text-sm">
          <Link
            className="rounded-lg bg-accent px-3 py-2 font-semibold text-white"
            href={`/reports/new?brandId=${brand.id}`}
          >
            정보 수정 제보하기
          </Link>
          {brand.officialUrl ? (
            <a
              className="rounded-lg border border-border px-3 py-2 font-semibold"
              href={brand.officialUrl}
              target="_blank"
              rel="noreferrer"
            >
              공식 홈페이지
            </a>
          ) : null}
          {brand.membershipUrl ? (
            <a
              className="rounded-lg border border-border px-3 py-2 font-semibold"
              href={brand.membershipUrl}
              target="_blank"
              rel="noreferrer"
            >
              멤버십 안내
            </a>
          ) : null}
          {brand.appUrl ? (
            <a
              className="rounded-lg border border-border px-3 py-2 font-semibold"
              href={brand.appUrl}
              target="_blank"
              rel="noreferrer"
            >
              앱 안내
            </a>
          ) : null}
        </div>
      </section>

      <section>
        <SectionHeader
          title={`${brand.name} 공개 혜택`}
          description="공식 출처 검수를 거쳐 공개된 생일 혜택만 표시합니다."
        />
        {brand.benefits.length > 0 ? (
          <div className="grid gap-4">
            {brand.benefits.map((benefit) => (
              <BenefitCard
                key={benefit.id}
                benefit={benefit}
                reportHref={`/reports/new?brandId=${brand.id}&benefitId=${benefit.id}`}
              />
            ))}
          </div>
        ) : (
          <EmptyState
            title="아직 공개된 생일 혜택이 없습니다."
            description="공식 출처 확인 후 업데이트될 예정입니다."
          />
        )}
      </section>

      <p className="border-t border-border pt-6 text-sm leading-7 text-neutral-500">
        브랜드 정책은 수시로 변경될 수 있습니다. 사용 전 공식 앱 또는 공식 페이지에서 최종 확인해 주세요.
      </p>
    </div>
  );
}
