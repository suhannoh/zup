import Link from "next/link";
import { getCategoryTheme } from "@/lib/categoryTheme";
import type { BrandListItem } from "@/types/brand";

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase();
}

export function BrandCard({ brand }: { brand: BrandListItem }) {
  const theme = getCategoryTheme(brand.categorySlug);

  return (
    <article className="rounded-xl border border-border bg-white p-5 shadow-sm transition duration-200 hover:-translate-y-1 hover:border-blue-200 hover:shadow-lg focus-within:border-blue-300 focus-within:shadow-lg">
      <div className="flex items-start gap-3">
        <div
          className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-base font-bold ${theme.icon}`}
          aria-hidden="true"
        >
          {getInitial(brand.name)}
        </div>
        <div className="min-w-0">
          <p className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${theme.badge}`}>
            {brand.categoryName}
          </p>
          <h3 className="mt-1 truncate text-lg font-bold text-neutral-950">{brand.name}</h3>
        </div>
      </div>
      <p className="mt-4 min-h-12 text-sm leading-6 text-neutral-600">
        {brand.description ?? "공식 출처 기준으로 생일 혜택 정보를 확인 중입니다."}
      </p>
      <Link
        href={`/brands/${brand.slug}`}
        className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-accent transition hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
      >
        상세 보기
        <span aria-hidden="true">→</span>
      </Link>
    </article>
  );
}
