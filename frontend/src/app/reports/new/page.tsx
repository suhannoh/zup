import type { Metadata } from "next";
import { ReportForm } from "@/components/public/ReportForm";
import { getBrands } from "@/lib/api/publicApi";
import type { BrandListItem } from "@/types/brand";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "정보 수정 제보 - Zup",
  description: "잘못된 생일 혜택 정보, 종료된 혜택, 새 공식 링크를 Zup에 제보해 주세요.",
  alternates: {
    canonical: "/reports/new",
  },
};

type NewReportPageProps = {
  searchParams: Promise<{
    brandId?: string;
    benefitId?: string;
  }>;
};

async function safeBrands() {
  try {
    return await getBrands();
  } catch {
    return [] as BrandListItem[];
  }
}

function parseNumber(value?: string) {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export default async function NewReportPage({ searchParams }: NewReportPageProps) {
  const [brands, params] = await Promise.all([safeBrands(), searchParams]);
  const brandId = parseNumber(params.brandId);
  const benefitId = parseNumber(params.benefitId);

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <section>
        <p className="text-sm font-semibold text-accent">정보 제보하기</p>
        <h1 className="mt-3 text-3xl font-bold text-neutral-950">혜택 정보를 알려주세요</h1>
        <p className="mt-4 text-sm leading-7 text-neutral-600">
          Zup은 공식 출처 기준으로 혜택 정보를 관리합니다. 잘못된 정보, 종료된 혜택, 새로 발견한 공식
          링크를 알려주시면 검수 후 반영하겠습니다.
        </p>
      </section>

      <ReportForm brands={brands} initialBrandId={brandId} initialBenefitId={benefitId} />
    </div>
  );
}
