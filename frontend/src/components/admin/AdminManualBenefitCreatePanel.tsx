"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  createAdminManualBenefit,
  getAdminBrands,
  getSourceWatches,
} from "@/lib/api/adminApi";
import {
  BENEFIT_TYPE_LABELS,
  BENEFIT_TYPE_OPTIONS,
  BIRTHDAY_TIMING_LABELS,
  BIRTHDAY_TIMING_OPTIONS,
  OCCASION_TYPE_LABELS,
  OCCASION_TYPE_OPTIONS,
  VERIFICATION_STATUS_LABELS,
} from "@/lib/benefitLabels";
import { SOURCE_TYPE_LABELS, SOURCE_TYPE_OPTIONS } from "@/lib/sourceLabels";
import type {
  AdminManualBenefitCreateRequest,
  BenefitType,
  BirthdayTimingType,
  OccasionType,
  VerificationStatus,
} from "@/types/adminBenefit";
import type { AdminBrand } from "@/types/adminBrand";
import type { SourceType } from "@/types/adminBenefitSource";
import type { SourceWatch } from "@/types/sourceWatch";

type DetailItemForm = {
  brandName: string;
  title: string;
  description: string;
  conditionText: string;
  imageUrl: string;
  displayOrder: number;
  isActive: boolean;
};

type SourceForm = {
  sourceType: SourceType;
  sourceUrl: string;
  sourceTitle: string;
  sourceCheckedAt: string;
  memo: string;
  collectionMethod: "MANUAL_VERIFIED";
};

type FormState = {
  brandId: string;
  title: string;
  summary: string;
  detail: string;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  usagePeriodDescription: string;
  requiredApp: boolean;
  requiredSignup: boolean;
  requiredMembership: boolean;
  requiredPurchase: boolean;
  conditionSummary: string;
  caution: string;
  verificationStatus: Extract<VerificationStatus, "VERIFIED" | "PUBLISHED">;
  isActive: boolean;
  detailItems: DetailItemForm[];
  sources: SourceForm[];
};

function today() {
  return new Date().toISOString().slice(0, 10);
}

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

const emptyForm: FormState = {
  brandId: "",
  title: "",
  summary: "",
  detail: "",
  benefitType: "COUPON",
  occasionType: "BIRTHDAY",
  birthdayTimingType: "UNKNOWN",
  usagePeriodDescription: "",
  requiredApp: false,
  requiredSignup: false,
  requiredMembership: true,
  requiredPurchase: false,
  conditionSummary: "",
  caution: "",
  verificationStatus: "VERIFIED",
  isActive: true,
  detailItems: [
    {
      brandName: "",
      title: "",
      description: "",
      conditionText: "",
      imageUrl: "",
      displayOrder: 1,
      isActive: true,
    },
  ],
  sources: [
    {
      sourceType: "OFFICIAL_HOME",
      sourceUrl: "",
      sourceTitle: "",
      sourceCheckedAt: today(),
      memo: "관리자가 공식 페이지 직접 확인",
      collectionMethod: "MANUAL_VERIFIED",
    },
  ],
};

const publishBlockedTerms = ["확인 필요", "임시", "테스트", "TODO", "미정"];

export function AdminManualBenefitCreatePanel() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [brands, setBrands] = useState<AdminBrand[]>([]);
  const [sourceWatches, setSourceWatches] = useState<SourceWatch[]>([]);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const [brandData, sourceWatchData] = await Promise.all([getAdminBrands(), getSourceWatches()]);
        setBrands(brandData ?? []);
        setSourceWatches(sourceWatchData ?? []);
      } catch {
        setError("등록 화면 정보를 불러오지 못했습니다.");
      }
    }

    load();
  }, []);

  useEffect(() => {
    const brandId = searchParams.get("brandId");
    const sourceWatchId = Number(searchParams.get("sourceWatchId") ?? "");
    const sourceWatch = sourceWatches.find((item) => item.id === sourceWatchId);
    if (!brandId && !sourceWatch) {
      return;
    }
    setForm((current) => ({
      ...current,
      brandId: sourceWatch ? String(sourceWatch.brandId) : brandId ?? current.brandId,
      sources: [
        {
          sourceType: sourceWatch?.sourceType ?? current.sources[0]?.sourceType ?? "OFFICIAL_HOME",
          sourceUrl: sourceWatch?.url ?? current.sources[0]?.sourceUrl ?? "",
          sourceTitle: sourceWatch?.title ?? current.sources[0]?.sourceTitle ?? "",
          sourceCheckedAt: today(),
          memo: sourceWatch
            ? "robots.txt 차단으로 자동 수집 대신 수동 등록"
            : current.sources[0]?.memo ?? "관리자가 공식 페이지 직접 확인",
          collectionMethod: "MANUAL_VERIFIED",
        },
      ],
    }));
  }, [searchParams, sourceWatches]);

  const publishBlocked = useMemo(() => {
    const text = `${form.title}\n${form.summary}\n${form.detail}\n${form.conditionSummary}\n${form.caution}`;
    return publishBlockedTerms.filter((term) => text.includes(term));
  }, [form]);

  function update(next: Partial<FormState>) {
    setForm((current) => ({ ...current, ...next }));
  }

  function updateDetailItem(index: number, patch: Partial<DetailItemForm>) {
    setForm((current) => ({
      ...current,
      detailItems: current.detailItems.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item),
    }));
  }

  function addDetailItem() {
    setForm((current) => ({
      ...current,
      detailItems: [
        ...current.detailItems,
        { brandName: "", title: "", description: "", conditionText: "", imageUrl: "", displayOrder: current.detailItems.length + 1, isActive: true },
      ],
    }));
  }

  function removeDetailItem(index: number) {
    setForm((current) => ({
      ...current,
      detailItems: current.detailItems.filter((_, itemIndex) => itemIndex !== index).map((item, itemIndex) => ({ ...item, displayOrder: itemIndex + 1 })),
    }));
  }

  function updateSource(index: number, patch: Partial<SourceForm>) {
    setForm((current) => ({
      ...current,
      sources: current.sources.map((source, sourceIndex) => sourceIndex === index ? { ...source, ...patch } : source),
    }));
  }

  function toRequest(): AdminManualBenefitCreateRequest {
    return {
      brandId: Number(form.brandId),
      title: form.title.trim(),
      summary: form.summary.trim(),
      detail: normalizeOptional(form.detail),
      benefitType: form.benefitType,
      occasionType: form.occasionType,
      birthdayTimingType: form.birthdayTimingType,
      conditionSummary: normalizeOptional(form.conditionSummary),
      requiredApp: form.requiredApp,
      requiredSignup: form.requiredSignup,
      requiredMembership: form.requiredMembership,
      requiredPurchase: form.requiredPurchase,
      usagePeriodDescription: normalizeOptional(form.usagePeriodDescription),
      caution: normalizeOptional(form.caution),
      verificationStatus: form.verificationStatus,
      lastVerifiedAt: form.sources[0]?.sourceCheckedAt || null,
      isActive: form.isActive,
      detailItems: form.detailItems
        .filter((item) => item.title.trim())
        .map((item, index) => ({
          brandName: normalizeOptional(item.brandName),
          title: item.title.trim(),
          description: normalizeOptional(item.description),
          conditionText: normalizeOptional(item.conditionText),
          imageUrl: normalizeOptional(item.imageUrl),
          displayOrder: index + 1,
          isActive: item.isActive,
        })),
      sources: form.sources.map((source) => ({
        sourceType: source.sourceType,
        sourceUrl: source.sourceUrl.trim(),
        sourceTitle: normalizeOptional(source.sourceTitle),
        sourceCheckedAt: source.sourceCheckedAt,
        memo: normalizeOptional(source.memo),
        collectionMethod: source.collectionMethod,
        verificationSummary: "관리자가 공식 페이지를 직접 확인해 수동 등록",
      })),
    };
  }

  function validate() {
    if (!form.brandId || !form.title.trim() || !form.summary.trim()) {
      return "브랜드, 제목, 요약은 필수입니다.";
    }
    if (form.sources.some((source) => !source.sourceUrl.trim() || !source.sourceCheckedAt)) {
      return "공식 출처 URL과 확인일은 필수입니다.";
    }
    if (form.verificationStatus === "PUBLISHED" && publishBlocked.length > 0) {
      return `공개 중 저장을 막는 표현이 있습니다: ${publishBlocked.join(", ")}`;
    }
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationMessage = validate();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const created = await createAdminManualBenefit(toRequest());
      setMessage("수동 등록된 혜택입니다. 공식 출처와 조건을 확인한 뒤 공개 상태를 관리하세요.");
      router.push(`/admin/benefits/${created.id}`);
    } catch {
      setError("수동 혜택 등록에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="space-y-6">
      <div>
        <Link className="text-sm font-semibold text-blue-600 hover:underline" href="/admin/benefits">
          혜택 목록으로 돌아가기
        </Link>
        <h1 className="mt-3 text-2xl font-bold text-neutral-950">수동 혜택 등록</h1>
        <p className="mt-2 text-sm text-neutral-600">
          robots.txt 차단 또는 자동 추출이 어려운 공식 출처는 관리자가 직접 확인한 뒤 혜택을 등록합니다.
        </p>
      </div>
      {message ? <Notice tone="success">{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}
      {publishBlocked.length > 0 ? (
        <Notice tone="warning">공개 중 저장 차단 표현: {publishBlocked.join(", ")}</Notice>
      ) : null}

      <form className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]" onSubmit={handleSubmit}>
        <div className="space-y-5">
          <Panel title="기본 정보">
            <Select label="브랜드" value={form.brandId} onChange={(brandId) => update({ brandId })} required>
              <option value="">선택</option>
              {brands.map((brand) => (
                <option key={brand.id} value={brand.id}>{brand.name}</option>
              ))}
            </Select>
            <TextInput label="혜택 제목" value={form.title} onChange={(title) => update({ title })} required />
            <TextArea label="요약" value={form.summary} onChange={(summary) => update({ summary })} required />
            <TextArea label="상세 설명" value={form.detail} onChange={(detail) => update({ detail })} />
            <div className="grid gap-3 md:grid-cols-3">
              <EnumSelect label="혜택 유형" value={form.benefitType} values={BENEFIT_TYPE_OPTIONS} labels={BENEFIT_TYPE_LABELS} onChange={(benefitType) => update({ benefitType })} />
              <EnumSelect label="적용 시점" value={form.occasionType} values={OCCASION_TYPE_OPTIONS} labels={OCCASION_TYPE_LABELS} onChange={(occasionType) => update({ occasionType })} />
              <EnumSelect label="생일 지급 시점" value={form.birthdayTimingType} values={BIRTHDAY_TIMING_OPTIONS} labels={BIRTHDAY_TIMING_LABELS} onChange={(birthdayTimingType) => update({ birthdayTimingType })} />
            </div>
            <TextInput label="사용 가능 기간" value={form.usagePeriodDescription} onChange={(usagePeriodDescription) => update({ usagePeriodDescription })} />
          </Panel>

          <Panel title="조건 정보">
            <div className="grid gap-2 md:grid-cols-4">
              <Checkbox label="앱 필요" checked={form.requiredApp} onChange={(requiredApp) => update({ requiredApp })} />
              <Checkbox label="회원가입 필요" checked={form.requiredSignup} onChange={(requiredSignup) => update({ requiredSignup })} />
              <Checkbox label="멤버십 필요" checked={form.requiredMembership} onChange={(requiredMembership) => update({ requiredMembership })} />
              <Checkbox label="구매 조건 있음" checked={form.requiredPurchase} onChange={(requiredPurchase) => update({ requiredPurchase })} />
            </div>
            <TextInput label="구매 조건" value={form.conditionSummary} onChange={(conditionSummary) => update({ conditionSummary })} />
            <TextArea label="이용 조건 / 주의사항" value={form.caution} onChange={(caution) => update({ caution })} />
          </Panel>

          <Panel title="대표 혜택 리스트">
            <p className="rounded-lg bg-neutral-50 p-3 text-xs leading-5 text-neutral-600">
              이미지 URL은 admin 검수 참고용입니다. public 화면에는 직접 노출하지 않습니다.
            </p>
            <div className="space-y-3">
              {form.detailItems.map((item, index) => (
                <div key={index} className="rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <span className="text-sm font-semibold">{index + 1}번 혜택</span>
                    <button className="text-sm font-semibold text-red-600" type="button" onClick={() => removeDetailItem(index)}>
                      삭제
                    </button>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <TextInput label="브랜드명" value={item.brandName} onChange={(brandName) => updateDetailItem(index, { brandName })} />
                    <TextInput label="혜택명" value={item.title} onChange={(title) => updateDetailItem(index, { title })} />
                    <TextArea label="설명" value={item.description} onChange={(description) => updateDetailItem(index, { description })} />
                    <TextArea label="사용 조건" value={item.conditionText} onChange={(conditionText) => updateDetailItem(index, { conditionText })} />
                    <TextInput label="이미지 URL" value={item.imageUrl} onChange={(imageUrl) => updateDetailItem(index, { imageUrl })} type="url" />
                    <Checkbox label="활성" checked={item.isActive} onChange={(isActive) => updateDetailItem(index, { isActive })} />
                  </div>
                </div>
              ))}
            </div>
            <button className="h-10 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={addDetailItem}>
              대표 혜택 추가
            </button>
          </Panel>
        </div>

        <aside className="space-y-5 xl:sticky xl:top-28 xl:self-start">
          <Panel title="공식 출처">
            {form.sources.map((source, index) => (
              <div key={index} className="space-y-3">
                <Select label="출처 유형" value={source.sourceType} onChange={(sourceType) => updateSource(index, { sourceType: sourceType as SourceType })}>
                  {SOURCE_TYPE_OPTIONS.map((type) => (
                    <option key={type} value={type}>{SOURCE_TYPE_LABELS[type]}</option>
                  ))}
                </Select>
                <TextInput label="출처 URL" value={source.sourceUrl} onChange={(sourceUrl) => updateSource(index, { sourceUrl })} required type="url" />
                <TextInput label="출처 제목" value={source.sourceTitle} onChange={(sourceTitle) => updateSource(index, { sourceTitle })} />
                <TextInput label="확인일" value={source.sourceCheckedAt} onChange={(sourceCheckedAt) => updateSource(index, { sourceCheckedAt })} required type="date" />
                <TextArea label="메모" value={source.memo} onChange={(memo) => updateSource(index, { memo })} />
                <Info label="검증 방식" value="MANUAL_VERIFIED" />
              </div>
            ))}
          </Panel>

          <Panel title="저장 상태">
            <Select label="저장 상태" value={form.verificationStatus} onChange={(verificationStatus) => update({ verificationStatus: verificationStatus as FormState["verificationStatus"] })}>
              <option value="VERIFIED">{VERIFICATION_STATUS_LABELS.VERIFIED}</option>
              <option value="PUBLISHED">{VERIFICATION_STATUS_LABELS.PUBLISHED}</option>
            </Select>
            <Checkbox label="활성 상태" checked={form.isActive} onChange={(isActive) => update({ isActive })} />
            <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
              기본값은 검증 완료입니다. 공개 중 저장은 공식 출처와 확인일, 금지어 검사를 통과해야 합니다.
            </p>
            <button className="h-11 w-full rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-60" type="submit" disabled={saving}>
              {saving ? "등록 중" : "수동 혜택 등록"}
            </button>
          </Panel>
        </aside>
      </form>
    </section>
  );
}

function Panel({ children, title }: { children: React.ReactNode; title: string }) {
  return (
    <section className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-bold text-neutral-950">{title}</h2>
      {children}
    </section>
  );
}

function Select({ children, label, onChange, required, value }: { children: React.ReactNode; label: string; onChange: (value: string) => void; required?: boolean; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <select className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required={required}>
        {children}
      </select>
    </label>
  );
}

function EnumSelect<T extends string>({ label, labels, onChange, value, values }: { label: string; labels: Record<T, string>; onChange: (value: T) => void; value: T; values: T[] }) {
  return (
    <Select label={label} value={value} onChange={(next) => onChange(next as T)}>
      {values.map((item) => (
        <option key={item} value={item}>{labels[item]}</option>
      ))}
    </Select>
  );
}

function TextInput({ label, onChange, required, type = "text", value }: { label: string; onChange: (value: string) => void; required?: boolean; type?: string; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <input className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required={required} type={type} />
    </label>
  );
}

function TextArea({ label, onChange, required, value }: { label: string; onChange: (value: string) => void; required?: boolean; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <textarea className="min-h-24 w-full rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm leading-6" value={value} onChange={(event) => onChange(event.target.value)} required={required} />
    </label>
  );
}

function Checkbox({ checked, label, onChange }: { checked: boolean; label: string; onChange: (value: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 text-sm font-medium">
      <input checked={checked} onChange={(event) => onChange(event.target.checked)} type="checkbox" />
      {label}
    </label>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-sm">
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 text-neutral-800">{value}</dd>
    </div>
  );
}

function Notice({ children, tone }: { children: React.ReactNode; tone: "success" | "error" | "warning" }) {
  const className = tone === "success"
    ? "border-green-200 bg-green-50 text-green-700"
    : tone === "warning"
      ? "border-amber-200 bg-amber-50 text-amber-800"
      : "border-red-200 bg-red-50 text-red-700";
  return <div className={`rounded-2xl border p-4 text-sm ${className}`}>{children}</div>;
}
