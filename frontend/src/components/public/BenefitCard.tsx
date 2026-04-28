import type { BenefitListItem } from "@/types/benefit";
import { TagPill } from "./TagPill";

const timingLabels: Record<string, string> = {
  BIRTHDAY_ONLY: "생일 당일",
  BIRTHDAY_MONTH: "생일월",
  BEFORE_AFTER_DAYS: "생일 전후",
  ISSUED_BEFORE_BIRTHDAY: "생일 전 발급",
  UNKNOWN: "기간 확인 필요",
};

const typeLabels: Record<string, string> = {
  FREE_ITEM: "무료",
  DISCOUNT: "할인",
  COUPON: "쿠폰",
  POINT: "포인트",
  GIFT: "선물",
  UPGRADE: "업그레이드",
  ETC: "기타",
};

function boolLabel(value: boolean) {
  return value ? "필요" : "불필요";
}

type BenefitCardProps = {
  benefit: BenefitListItem;
  reportHref?: string;
};

export function BenefitCard({ benefit, reportHref }: BenefitCardProps) {
  const primarySource = benefit.sources[0];

  return (
    <article className="rounded-xl border border-border bg-white p-5 shadow-sm">
      <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
        <div>
          <p className="text-xs font-semibold text-accent">{benefit.brandName}</p>
          <h3 className="mt-1 text-lg font-bold text-neutral-950">{benefit.title}</h3>
        </div>
        <span className="w-fit rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
          공식 확인
        </span>
      </div>
      <p className="mt-3 text-sm leading-6 text-neutral-600">{benefit.summary}</p>
      <dl className="mt-5 grid gap-3 rounded-lg bg-neutral-50 p-4 text-sm md:grid-cols-2">
        <div>
          <dt className="text-xs font-semibold text-neutral-500">혜택 유형</dt>
          <dd className="mt-1 font-medium">{typeLabels[benefit.benefitType] ?? benefit.benefitType}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">사용 기간</dt>
          <dd className="mt-1 font-medium">{timingLabels[benefit.birthdayTimingType] ?? benefit.birthdayTimingType}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">앱</dt>
          <dd className="mt-1 font-medium">{boolLabel(benefit.requiredApp)}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">멤버십</dt>
          <dd className="mt-1 font-medium">{boolLabel(benefit.requiredMembership)}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">구매 조건</dt>
          <dd className="mt-1 font-medium">{boolLabel(benefit.requiredPurchase)}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">최근 확인일</dt>
          <dd className="mt-1 font-medium">{benefit.lastVerifiedAt ?? "확인 예정"}</dd>
        </div>
      </dl>
      {benefit.usagePeriodDescription ? (
        <p className="mt-4 text-sm leading-6 text-neutral-600">{benefit.usagePeriodDescription}</p>
      ) : null}
      {benefit.tags.length > 0 ? (
        <div className="mt-4 flex flex-wrap gap-2">
          {benefit.tags.map((tag) => (
            <TagPill key={tag.slug} tag={tag} />
          ))}
        </div>
      ) : null}
      <div className="mt-5 border-t border-border pt-4 text-sm">
        <div className="flex flex-wrap items-center gap-3">
          {primarySource ? (
            <a
              className="font-semibold text-accent hover:text-blue-700"
              href={primarySource.sourceUrl}
              target="_blank"
              rel="noreferrer"
            >
              {primarySource.sourceTitle ?? "공식 출처 확인하기"}
            </a>
          ) : (
            <span className="text-neutral-500">공식 출처 확인 예정</span>
          )}
          {reportHref ? (
            <a className="font-semibold text-neutral-500 hover:text-accent" href={reportHref}>
              이 혜택 제보하기
            </a>
          ) : null}
        </div>
      </div>
    </article>
  );
}
