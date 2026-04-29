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

function createEmptySource(checkedAt = "", memo = ""): SourceForm {
  return {
    sourceType: "OFFICIAL_HOME",
    sourceUrl: "",
    sourceTitle: "",
    sourceCheckedAt: checkedAt,
    memo,
    collectionMethod: "MANUAL_VERIFIED",
  };
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
    createEmptySource(today(), "관리자가 공식 페이지 직접 확인"),
  ],
};

const publishBlockedTerms = ["확인 필요", "임시", "테스트", "TODO", "미정"];

type ChecklistItem = {
  label: string;
  ok: boolean;
  warning: string;
};

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

  const selectedBrand = useMemo(
    () => brands.find((brand) => String(brand.id) === form.brandId) ?? null,
    [brands, form.brandId],
  );

  const activePreviewItems = useMemo(
    () => form.detailItems.filter((item) => item.isActive && item.title.trim().length > 0),
    [form.detailItems],
  );

  const validSources = useMemo(
    () => form.sources.filter((source) => source.sourceUrl.trim().length > 0 && source.sourceCheckedAt.length > 0),
    [form.sources],
  );

  const partialSources = useMemo(
    () => form.sources.filter((source) => {
      const hasUrl = source.sourceUrl.trim().length > 0;
      const hasCheckedAt = source.sourceCheckedAt.length > 0;
      const hasAnyInput = hasUrl || hasCheckedAt || source.sourceTitle.trim().length > 0 || source.memo.trim().length > 0;
      return hasAnyInput && (!hasUrl || !hasCheckedAt);
    }),
    [form.sources],
  );

  const duplicateSourceUrls = useMemo(() => {
    const counts = new Map<string, number>();
    form.sources.forEach((source) => {
      const url = source.sourceUrl.trim();
      if (url.length === 0) {
        return;
      }
      counts.set(url, (counts.get(url) ?? 0) + 1);
    });
    return Array.from(counts.entries()).filter(([, count]) => count > 1).map(([url]) => url);
  }, [form.sources]);

  const checklist = useMemo<ChecklistItem[]>(() => [
    {
      label: "브랜드 선택됨",
      ok: Boolean(form.brandId && selectedBrand),
      warning: "브랜드를 선택하세요",
    },
    {
      label: "제목 입력됨",
      ok: form.title.trim().length > 0,
      warning: "혜택 제목을 입력하세요",
    },
    {
      label: "요약 입력됨",
      ok: form.summary.trim().length > 0,
      warning: "요약을 입력하세요",
    },
    {
      label: "대표 혜택 1개 이상",
      ok: activePreviewItems.length > 0,
      warning: "대표 혜택이 없습니다",
    },
    {
      label: "공식 출처 1개 이상",
      ok: validSources.length > 0,
      warning: "공식 출처 URL을 입력하세요",
    },
    {
      label: "확인일 입력됨",
      ok: validSources.length > 0,
      warning: "최종 확인일을 입력하세요",
    },
    {
      label: "금지어 없음",
      ok: publishBlocked.length === 0,
      warning: publishBlocked.length > 0 ? `차단 표현: ${publishBlocked.join(", ")}` : "금지어를 확인하세요",
    },
  ], [activePreviewItems.length, form.brandId, form.summary, form.title, publishBlocked, selectedBrand, validSources.length]);

  const publishedReady = checklist.every((item) => item.ok);

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

  function addSource() {
    setForm((current) => ({
      ...current,
      sources: [...current.sources, createEmptySource()],
    }));
  }

  function removeSource(index: number) {
    setForm((current) => {
      if (current.sources.length <= 1) {
        return current;
      }
      return {
        ...current,
        sources: current.sources.filter((_, sourceIndex) => sourceIndex !== index),
      };
    });
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
      lastVerifiedAt: validSources[0]?.sourceCheckedAt || null,
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
      sources: validSources.map((source) => ({
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
    if (validSources.length === 0) {
      return "공식 출처를 1개 이상 입력해야 합니다.";
    }
    if (partialSources.length > 0) {
      return "입력 중인 공식 출처에는 URL과 확인일을 모두 입력해야 합니다.";
    }
    if (form.verificationStatus === "PUBLISHED" && !publishedReady) {
      return "공개 중으로 저장하려면 공식 출처와 필수 정보를 모두 입력해야 합니다.";
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

  const saveHelp = form.verificationStatus === "PUBLISHED"
    ? "공개 중 상태로 저장됩니다. 사용자 브랜드 페이지에 바로 노출됩니다."
    : "검증 완료 상태로 저장됩니다. 사용자 화면에는 아직 노출되지 않습니다.";
  const disableSubmit = saving || (form.verificationStatus === "PUBLISHED" && !publishedReady);

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
          <ManualBenefitPreview
            checklist={checklist}
            detailItems={activePreviewItems}
            form={form}
            isPublishedMode={form.verificationStatus === "PUBLISHED"}
            publishedReady={publishedReady}
            selectedBrand={selectedBrand}
            validSources={validSources}
          />

          <Panel title="공식 출처">
            {validSources.length === 0 ? (
              <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
                공식 출처를 1개 이상 입력해야 합니다.
              </p>
            ) : null}
            {duplicateSourceUrls.length > 0 ? (
              <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
                중복된 출처 URL이 있습니다: {duplicateSourceUrls.join(", ")}
              </p>
            ) : null}
            {form.sources.map((source, index) => (
              <div key={index} className="space-y-3 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold text-neutral-900">
                      {index === 0 ? "대표 출처" : `${index + 1}번 출처`}
                    </div>
                    <p className="mt-1 text-xs text-neutral-500">첫 번째 유효 출처는 대표 출처로 저장됩니다.</p>
                  </div>
                  <button
                    className="text-sm font-semibold text-red-600 disabled:text-neutral-400"
                    disabled={form.sources.length <= 1}
                    onClick={() => removeSource(index)}
                    type="button"
                  >
                    삭제
                  </button>
                </div>
                <SourceInputErrors source={source} />
                <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-1">
                  <Select label="출처 유형" value={source.sourceType} onChange={(sourceType) => updateSource(index, { sourceType: sourceType as SourceType })}>
                    {SOURCE_TYPE_OPTIONS.map((type) => (
                      <option key={type} value={type}>{SOURCE_TYPE_LABELS[type]}</option>
                    ))}
                  </Select>
                  <TextInput label="출처 URL" value={source.sourceUrl} onChange={(sourceUrl) => updateSource(index, { sourceUrl })} type="url" />
                  <TextInput label="출처 제목" value={source.sourceTitle} onChange={(sourceTitle) => updateSource(index, { sourceTitle })} />
                  <TextInput label="확인일" value={source.sourceCheckedAt} onChange={(sourceCheckedAt) => updateSource(index, { sourceCheckedAt })} type="date" />
                </div>
                <TextArea label="메모" value={source.memo} onChange={(memo) => updateSource(index, { memo })} />
                <Info label="검증 방식" value="MANUAL_VERIFIED" />
              </div>
            ))}
            <button className="h-10 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={addSource}>
              출처 추가
            </button>
          </Panel>

          <Panel title="저장 상태">
            <Select label="저장 상태" value={form.verificationStatus} onChange={(verificationStatus) => update({ verificationStatus: verificationStatus as FormState["verificationStatus"] })}>
              <option value="VERIFIED">{VERIFICATION_STATUS_LABELS.VERIFIED}</option>
              <option value="PUBLISHED">{VERIFICATION_STATUS_LABELS.PUBLISHED}</option>
            </Select>
            <Checkbox label="활성 상태" checked={form.isActive} onChange={(isActive) => update({ isActive })} />
            <p className="rounded-lg bg-neutral-50 p-3 text-xs leading-5 text-neutral-700">
              {saveHelp}
            </p>
            {form.verificationStatus === "PUBLISHED" && !publishedReady ? (
              <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
                공개 중으로 저장하려면 공식 출처와 필수 정보를 모두 입력해야 합니다.
              </p>
            ) : null}
            <button className="h-11 w-full rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-60" type="submit" disabled={disableSubmit}>
              {saving ? "등록 중" : "수동 혜택 등록"}
            </button>
          </Panel>
        </aside>
      </form>
    </section>
  );
}

function ManualBenefitPreview({
  checklist,
  detailItems,
  form,
  isPublishedMode,
  publishedReady,
  selectedBrand,
  validSources,
}: {
  checklist: ChecklistItem[];
  detailItems: DetailItemForm[];
  form: FormState;
  isPublishedMode: boolean;
  publishedReady: boolean;
  selectedBrand: AdminBrand | null;
  validSources: SourceForm[];
}) {
  const primarySource = validSources[0];
  const checkedAt = primarySource?.sourceCheckedAt || "";
  const brandName = selectedBrand?.name ?? "브랜드 선택 전";
  const visibleSources = validSources.slice(0, 3);
  const hiddenSourceCount = Math.max(validSources.length - visibleSources.length, 0);

  return (
    <Panel title="공개 화면 미리보기">
      <p className="text-sm leading-6 text-neutral-600">
        저장 전 사용자 브랜드 페이지에서 보일 내용을 미리 확인합니다.
      </p>

      <div className="rounded-2xl border border-neutral-200 bg-neutral-50 p-4">
        <div className="text-xs font-semibold text-neutral-500">{brandName}</div>
        <h3 className="mt-2 text-xl font-bold leading-7 text-neutral-950">
          {form.title.trim() || "혜택 제목을 입력하세요"}
        </h3>
        <p className="mt-2 text-sm leading-6 text-neutral-700">
          {form.summary.trim() || "요약을 입력하면 사용자 화면 미리보기에 표시됩니다."}
        </p>

        <div className="mt-4 flex flex-wrap gap-2">
          <PreviewPill>{BENEFIT_TYPE_LABELS[form.benefitType]}</PreviewPill>
          <PreviewPill>{OCCASION_TYPE_LABELS[form.occasionType]}</PreviewPill>
          <PreviewPill>{BIRTHDAY_TIMING_LABELS[form.birthdayTimingType]}</PreviewPill>
        </div>

        <div className="mt-4 grid gap-2 text-xs text-neutral-700 sm:grid-cols-3">
          <PreviewFlag label="앱 필요" value={form.requiredApp} />
          <PreviewFlag label="회원가입 필요" value={form.requiredSignup} />
          <PreviewFlag label="멤버십 필요" value={form.requiredMembership} />
        </div>

        <div className="mt-5 space-y-3">
          <PreviewSection title="대표 혜택">
            {detailItems.length > 0 ? (
              <ul className="space-y-2">
                {detailItems.map((item, index) => (
                  <li key={`${item.title}-${index}`} className="rounded-lg bg-white p-3 text-sm">
                    <div className="font-semibold text-neutral-900">{item.title.trim()}</div>
                    {item.brandName.trim() ? <div className="mt-1 text-xs text-neutral-500">{item.brandName.trim()}</div> : null}
                    {item.description.trim() ? <p className="mt-2 leading-5 text-neutral-700">{item.description.trim()}</p> : null}
                    {item.conditionText.trim() ? <p className="mt-2 text-xs leading-5 text-neutral-500">{item.conditionText.trim()}</p> : null}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-neutral-500">대표 혜택이 아직 없습니다.</p>
            )}
          </PreviewSection>

          <PreviewSection title="조건 및 기간">
            <dl className="space-y-2 text-sm">
              <PreviewRow label="구매 조건" value={form.conditionSummary} fallback="구매 조건을 입력하면 표시됩니다." />
              <PreviewRow label="이용 조건" value={form.caution} fallback="이용 조건과 주의사항을 입력하면 표시됩니다." />
              <PreviewRow label="사용 가능 기간" value={form.usagePeriodDescription} fallback="사용 가능 기간을 입력하면 표시됩니다." />
              <PreviewRow label="최근 확인일" value={checkedAt} fallback="확인일을 입력하면 표시됩니다." />
            </dl>
          </PreviewSection>

          <PreviewSection title="공식 출처">
            {visibleSources.length > 0 ? (
              <ul className="space-y-2">
                {visibleSources.map((source, index) => (
                  <li key={`${source.sourceUrl}-${index}`}>
                    <a className="text-sm font-semibold text-blue-600 hover:underline" href={source.sourceUrl.trim()} rel="noreferrer" target="_blank">
                      {source.sourceTitle.trim() || `공식 출처 ${index + 1}`}
                    </a>
                  </li>
                ))}
                {hiddenSourceCount > 0 ? (
                  <li className="text-sm text-neutral-500">외 {hiddenSourceCount}개 출처</li>
                ) : null}
              </ul>
            ) : (
              <p className="text-sm text-neutral-500">공식 출처를 입력하면 출처 링크가 표시됩니다.</p>
            )}
          </PreviewSection>
        </div>
      </div>

      <div className={isPublishedMode ? "rounded-2xl border border-amber-200 bg-amber-50 p-4" : "rounded-2xl border border-neutral-200 bg-white p-4"}>
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-sm font-bold text-neutral-950">공개 전 체크</h3>
          {isPublishedMode ? (
            <span className={publishedReady ? "rounded-full bg-green-100 px-2 py-1 text-xs font-semibold text-green-700" : "rounded-full bg-amber-100 px-2 py-1 text-xs font-semibold text-amber-800"}>
              {publishedReady ? "공개 가능" : "조건 확인 필요"}
            </span>
          ) : null}
        </div>
        <ul className="mt-3 space-y-2">
          {checklist.map((item) => (
            <li key={item.label} className="flex items-start gap-2 text-xs leading-5">
              <span className={item.ok ? "rounded-full bg-green-100 px-2 py-0.5 font-semibold text-green-700" : "rounded-full bg-amber-100 px-2 py-0.5 font-semibold text-amber-800"}>
                {item.ok ? "통과" : "주의"}
              </span>
              <span className={item.ok ? "text-neutral-700" : "text-amber-900"}>
                {item.ok ? item.label : item.warning}
              </span>
            </li>
          ))}
        </ul>
        {isPublishedMode && !publishedReady ? (
          <p className="mt-3 rounded-lg bg-white p-3 text-xs leading-5 text-amber-900">
            공개 중으로 저장하려면 공식 출처와 필수 정보를 모두 입력해야 합니다.
          </p>
        ) : null}
      </div>
    </Panel>
  );
}

function SourceInputErrors({ source }: { source: SourceForm }) {
  const hasUrl = source.sourceUrl.trim().length > 0;
  const hasCheckedAt = source.sourceCheckedAt.length > 0;
  const hasContext = hasCheckedAt || source.sourceTitle.trim().length > 0 || source.memo.trim().length > 0;
  const errors = [
    !hasUrl && hasContext ? "출처 URL을 입력해 주세요." : null,
    hasUrl && !hasCheckedAt ? "확인일을 입력해 주세요." : null,
  ].filter((item): item is string => Boolean(item));

  if (errors.length === 0) {
    return null;
  }

  return (
    <ul className="space-y-1 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
      {errors.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}

function PreviewPill({ children }: { children: React.ReactNode }) {
  return <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-neutral-700">{children}</span>;
}

function PreviewFlag({ label, value }: { label: string; value: boolean }) {
  return (
    <div className="rounded-lg bg-white px-3 py-2">
      <span className="font-semibold">{label}</span>
      <span className="ml-2 text-neutral-500">{value ? "필요" : "해당 없음"}</span>
    </div>
  );
}

function PreviewSection({ children, title }: { children: React.ReactNode; title: string }) {
  return (
    <section>
      <h4 className="mb-2 text-xs font-bold text-neutral-500">{title}</h4>
      {children}
    </section>
  );
}

function PreviewRow({ fallback, label, value }: { fallback: string; label: string; value: string }) {
  const trimmed = value.trim();
  return (
    <div>
      <dt className="text-xs font-semibold text-neutral-500">{label}</dt>
      <dd className={trimmed ? "mt-1 text-neutral-800" : "mt-1 text-neutral-500"}>{trimmed || fallback}</dd>
    </div>
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
