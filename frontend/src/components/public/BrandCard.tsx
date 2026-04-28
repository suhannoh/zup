import Link from "next/link";
import type { BrandListItem } from "@/types/brand";

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase();
}

export function BrandCard({ brand }: { brand: BrandListItem }) {
  return (
    <article className="rounded-xl border border-border bg-white p-5 shadow-sm transition hover:border-blue-200 hover:shadow-md">
      <div className="flex items-start gap-3">
        <div
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-base font-bold text-white"
          style={{ backgroundColor: brand.brandColor ?? "#3B6FE8" }}
          aria-hidden="true"
        >
          {getInitial(brand.name)}
        </div>
        <div className="min-w-0">
          <p className="text-xs font-semibold text-accent">{brand.categoryName}</p>
          <h3 className="mt-1 truncate text-lg font-bold text-neutral-950">{brand.name}</h3>
        </div>
      </div>
      <p className="mt-4 min-h-12 text-sm leading-6 text-neutral-600">
        {brand.description ?? "공식 출처 기준으로 생일 혜택 정보를 확인 중입니다."}
      </p>
      <Link
        href={`/brands/${brand.slug}`}
        className="mt-5 inline-flex text-sm font-semibold text-accent hover:text-blue-700"
      >
        상세 보기
      </Link>
    </article>
  );
}
