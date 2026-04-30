import type { Metadata } from "next";
import { cache } from "react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { BenefitCard } from "@/components/public/BenefitCard";
import { EmptyState } from "@/components/public/EmptyState";
import { SectionHeader } from "@/components/public/SectionHeader";
import { getCategoryTheme } from "@/lib/categoryTheme";
import { getBrandDetail, isApiNotFound } from "@/lib/api/publicApi";
import type { BrandDetail } from "@/types/brand";

export const dynamic = "force-dynamic";

const getBrandDetailCached = cache((slug: string) => getBrandDetail(slug));

type BrandDetailPageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: BrandDetailPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const brand = await getBrandDetailCached(slug);
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
    brand = await getBrandDetailCached(slug);
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

  const benefits = brand.benefits ?? [];
  const theme = getCategoryTheme(brand.categorySlug);
  const initial = brand.name.trim().slice(0, 1).toUpperCase();

  return (
    <div className="space-y-10">
      <section className="relative overflow-hidden rounded-2xl border border-border bg-white p-6 shadow-sm">
        <div className={`absolute left-0 top-0 h-full w-1 ${theme.accent}`} aria-hidden="true" />
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
          <div
            className={`flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl text-xl font-bold ${theme.icon}`}
            aria-hidden="true"
          >
            {initial}
          </div>
          <div className="min-w-0 flex-1">
            <Link
              href={`/categories/${brand.categorySlug}`}
              className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${theme.badge}`}
            >
              {brand.categoryName}
            </Link>
            <h1 className="mt-3 text-3xl font-bold text-neutral-950">{brand.name}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-neutral-600">
              {brand.description ?? "공식 출처 기준으로 공개 가능한 생일 혜택 정보를 확인 중입니다."}
            </p>
            <div className="mt-5 flex flex-wrap gap-2 text-sm">
              <Link
                className="rounded-lg border border-blue-200 bg-white px-3 py-2 font-semibold text-blue-700 transition hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-200"
                href={`/reports/new?brandId=${brand.id}`}
              >
                정보 수정 제보하기
              </Link>
              {brand.officialUrl ? (
                <a
                  className="rounded-lg border border-border px-3 py-2 font-semibold transition hover:border-blue-200 hover:bg-blue-50"
                  href={brand.officialUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  공식 홈페이지
                </a>
              ) : null}
              {brand.membershipUrl ? (
                <a
                  className="rounded-lg border border-border px-3 py-2 font-semibold transition hover:border-blue-200 hover:bg-blue-50"
                  href={brand.membershipUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  멤버십 안내
                </a>
              ) : null}
              {brand.appUrl ? (
                <a
                  className="rounded-lg border border-border px-3 py-2 font-semibold transition hover:border-blue-200 hover:bg-blue-50"
                  href={brand.appUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  앱 안내
                </a>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section>
        <SectionHeader
          title={`${brand.name} 공개 혜택`}
          description="공식 출처 검수를 거쳐 공개된 생일 혜택만 표시합니다."
        />
        {benefits.length > 0 ? (
          <div className="grid gap-4">
            {benefits.map((benefit) => (
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
            description="공식 출처 확인 후 업데이트될 예정입니다. 알고 있는 혜택 정보가 있다면 제보해 주세요."
            icon="gift"
            actionHref={`/reports/new?brandId=${brand.id}`}
            actionLabel="혜택 정보 제보하기"
          />
        )}
      </section>
    </div>
  );
}
