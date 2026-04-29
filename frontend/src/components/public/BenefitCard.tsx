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
  FREE_ITEM: "무료 제공",
  DISCOUNT: "할인",
  COUPON: "쿠폰",
  POINT: "포인트",
  GIFT: "증정",
  UPGRADE: "업그레이드",
  ETC: "기타",
};

function boolLabel(value: boolean | null | undefined) {
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
  return timingLabels[benefit.birthdayTimingType] ?? benefit.birthdayTimingType ?? "공식 안내 확인";
}

type BenefitCardProps = {
  benefit: BenefitListItem;
  reportHref?: string;
};

export function BenefitCard({ benefit, reportHref }: BenefitCardProps) {
  const primarySource = benefit.sources?.[0];
  const conditionLines = splitLines(benefit.conditionSummary);
  const cautionLines = splitLines(benefit.caution);
  const detailItems = benefit.detailItems ?? [];
  const sourceLabel = primarySource?.sourceTitle ?? primarySource?.sourceUrl ?? "공식 출처 확인";
  const checkedAt = primarySource?.sourceCheckedAt ?? benefit.lastVerifiedAt;

  return (
    <article className="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
      <div className="border-b border-border bg-white p-5 md:p-6">
        <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-normal text-accent">{benefit.brandName}</p>
            <h3 className="mt-1 text-2xl font-bold leading-tight text-neutral-950">{benefit.title}</h3>
          </div>
          <span className="w-fit rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700 ring-1 ring-green-100">
            공식 확인
          </span>
        </div>
        <p className="mt-4 max-w-3xl text-sm leading-7 text-neutral-700">{benefit.summary}</p>
      </div>

      <div className="p-5 md:p-6">
        <section className="rounded-xl border border-blue-100 bg-blue-50/50 p-4 md:p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 className="text-base font-bold text-neutral-950">대표 혜택</h4>
            {detailItems.length > 0 ? (
              <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-blue-700 ring-1 ring-blue-100">
                {detailItems.length}개 항목
              </span>
            ) : null}
          </div>
          {detailItems.length > 0 ? (
            <ul className="mt-4 grid gap-3 md:grid-cols-2">
              {detailItems.map((item) => (
                <li key={item.id} className="rounded-lg border border-blue-100 bg-white p-4">
                  {item.brandName ? <p className="text-xs font-semibold text-blue-700">{item.brandName}</p> : null}
                  <p className="text-base font-bold leading-6 text-neutral-950">{item.title}</p>
                  {item.conditionText ? <p className="mt-1 text-sm leading-5 text-neutral-600">{item.conditionText}</p> : null}
                  {item.description ? <p className="mt-2 text-xs leading-5 text-neutral-500">{item.description}</p> : null}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-neutral-600">상세 혜택은 공식 안내를 확인해 주세요.</p>
          )}
        </section>

        <dl className="mt-5 grid gap-3 rounded-lg bg-neutral-50 p-4 text-sm md:grid-cols-3">
          <div>
            <dt className="text-xs font-semibold text-neutral-500">혜택 유형</dt>
            <dd className="mt-1 font-medium">{typeLabels[benefit.benefitType] ?? benefit.benefitType}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold text-neutral-500">생일 적용 기간</dt>
            <dd className="mt-1 font-medium">{timingLabels[benefit.birthdayTimingType] ?? benefit.birthdayTimingType}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold text-neutral-500">최근 확인일</dt>
            <dd className="mt-1 font-medium">{checkedAt ?? "확인 예정"}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold text-neutral-500">앱 필요</dt>
            <dd className="mt-1 font-medium">{boolLabel(benefit.requiredApp)}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold text-neutral-500">회원가입 필요</dt>
            <dd className="mt-1 font-medium">{benefit.requiredMembership ? "필요" : "공식 안내 확인"}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold text-neutral-500">멤버십 필요</dt>
            <dd className="mt-1 font-medium">{boolLabel(benefit.requiredMembership)}</dd>
          </div>
          {benefit.membershipGrade ? (
            <div>
              <dt className="text-xs font-semibold text-neutral-500">멤버십 등급</dt>
              <dd className="mt-1 font-medium">{benefit.membershipGrade}</dd>
            </div>
          ) : null}
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

        <div className="mt-4 grid gap-3 text-sm md:grid-cols-2">
          <InfoBox label="구매 조건" value={benefit.requiredPurchase ? "쿠폰별 최소 구매 금액과 사용 조건은 공식 안내를 확인하세요." : "공식 안내 확인"} />
          <InfoBox label="사용 가능 기간" value={formatPeriod(benefit)} />
        </div>

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

        {benefit.tags?.length > 0 ? (
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
                정보 수정 제보
              </a>
            ) : null}
          </div>
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
