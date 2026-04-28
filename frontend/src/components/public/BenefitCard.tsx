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

function splitLines(value: string | null | undefined) {
  if (!value) {
    return [];
  }
  return value
    .split(/\n|\. /)
    .map((line) => line.replace(/\.$/, "").trim())
    .filter(Boolean);
}

function formatPeriod(benefit: BenefitListItem) {
  if (benefit.usagePeriodDescription) {
    return benefit.usagePeriodDescription;
  }
  if (benefit.availableFrom || benefit.availableTo) {
    return `${benefit.availableFrom ?? "시작일 미정"} ~ ${benefit.availableTo ?? "종료일 미정"}`;
  }
  return timingLabels[benefit.birthdayTimingType] ?? benefit.birthdayTimingType;
}

type BenefitCardProps = {
  benefit: BenefitListItem;
  reportHref?: string;
};

export function BenefitCard({ benefit, reportHref }: BenefitCardProps) {
  const primarySource = benefit.sources[0];
  const conditionLines = splitLines(benefit.conditionSummary);
  const cautionLines = splitLines(benefit.caution);
  const detailItems = benefit.detailItems ?? [];
  const sourceLabel = primarySource?.sourceTitle ?? primarySource?.sourceUrl ?? "공식 출처 확인";
  const checkedAt = primarySource?.sourceCheckedAt ?? benefit.lastVerifiedAt;

  return (
    <article className="rounded-lg border border-border bg-white p-5 shadow-sm">
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
      {detailItems.length > 0 ? (
        <section className="mt-4 rounded-lg border border-border p-4">
          <h4 className="text-sm font-semibold text-neutral-900">대표 혜택</h4>
          <ul className="mt-2 space-y-2 text-sm leading-6 text-neutral-700">
            {detailItems.map((item) => (
              <li key={item.id}>
                - {item.brandName ? <span className="font-semibold">{item.brandName} · </span> : null}
                {item.title}
                {item.conditionText ? <span className="text-neutral-500"> ({item.conditionText})</span> : null}
                {item.description ? <p className="ml-3 text-neutral-500">{item.description}</p> : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      <dl className="mt-5 grid gap-3 rounded-lg bg-neutral-50 p-4 text-sm md:grid-cols-2">
        <div>
          <dt className="text-xs font-semibold text-neutral-500">혜택 유형</dt>
          <dd className="mt-1 font-medium">{typeLabels[benefit.benefitType] ?? benefit.benefitType}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">생일 적용 기간</dt>
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
          <dt className="text-xs font-semibold text-neutral-500">회원가입</dt>
          <dd className="mt-1 font-medium">{benefit.requiredMembership ? "필요" : "공식 안내 확인"}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold text-neutral-500">최소 구매 조건</dt>
          <dd className="mt-1 font-medium">{boolLabel(benefit.requiredPurchase)}</dd>
        </div>
        {benefit.membershipGrade ? (
          <div>
            <dt className="text-xs font-semibold text-neutral-500">멤버십 등급</dt>
            <dd className="mt-1 font-medium">{benefit.membershipGrade}</dd>
          </div>
        ) : null}
        <div>
          <dt className="text-xs font-semibold text-neutral-500">최근 확인일</dt>
          <dd className="mt-1 font-medium">{checkedAt ?? "확인 예정"}</dd>
        </div>
      </dl>

      {conditionLines.length > 0 ? (
        <section className="mt-4 rounded-lg border border-border p-4">
          <h4 className="text-sm font-semibold text-neutral-900">이용 조건</h4>
          <ul className="mt-2 space-y-1 text-sm leading-6 text-neutral-600">
            {conditionLines.map((line) => (
              <li key={line}>- {line}</li>
            ))}
          </ul>
        </section>
      ) : null}

      {benefit.requiredPurchase || benefit.usagePeriodDescription || benefit.availableFrom || benefit.availableTo ? (
        <div className="mt-4 grid gap-3 text-sm md:grid-cols-2">
          <InfoBox label="구매 조건" value={benefit.requiredPurchase ? "쿠폰별 최소 구매 금액과 사용 조건은 공식 안내를 확인하세요." : "별도 구매 조건 확인 필요"} />
          <InfoBox label="사용 가능 기간" value={formatPeriod(benefit)} />
        </div>
      ) : null}

      {cautionLines.length > 0 ? (
        <section className="mt-4 rounded-lg bg-amber-50 p-4">
          <h4 className="text-sm font-semibold text-amber-900">주의사항</h4>
          <ul className="mt-2 space-y-1 text-sm leading-6 text-amber-800">
            {cautionLines.map((line) => (
              <li key={line}>- {line}</li>
            ))}
          </ul>
        </section>
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
              공식 출처 확인: {sourceLabel}
            </a>
          ) : (
            <span className="text-neutral-500">공식 출처 확인 예정</span>
          )}
          {checkedAt ? <span className="text-neutral-500">최근 확인일: {checkedAt}</span> : null}
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

function InfoBox({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border p-4">
      <p className="text-xs font-semibold text-neutral-500">{label}</p>
      <p className="mt-2 text-sm leading-6 text-neutral-700">{value}</p>
    </div>
  );
}
