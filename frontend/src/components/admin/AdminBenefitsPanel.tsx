"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import Link from "next/link";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import {
  BIRTHDAY_TIMING_LABELS,
  BIRTHDAY_TIMING_OPTIONS,
  BENEFIT_TYPE_LABELS,
  BENEFIT_TYPE_OPTIONS,
  OCCASION_TYPE_LABELS,
  OCCASION_TYPE_OPTIONS,
  VERIFICATION_STATUS_LABELS,
  VERIFICATION_STATUS_OPTIONS,
} from "@/lib/benefitLabels";
import {
  createAdminBenefit,
  getAdminBenefit,
  getAdminBenefits,
  getAdminBrands,
  updateAdminBenefit,
  updateAdminBenefitActive,
  updateAdminBenefitStatus,
} from "@/lib/api/adminApi";
import { getCategories } from "@/lib/api/publicApi";
import type {
  AdminBenefit,
  AdminBenefitCreateRequest,
  AdminBenefitSummary,
  BenefitType,
  BirthdayTimingType,
  OccasionType,
  VerificationStatus,
} from "@/types/adminBenefit";
import type { AdminBrand } from "@/types/adminBrand";
import type { Category } from "@/types/category";

type ActiveFilter = "ALL" | "ACTIVE" | "INACTIVE";
type VisibilityFilter = "ALL" | "PUBLISHED_ACTIVE" | "VERIFIED" | "INACTIVE";

type BenefitFormState = {
  brandId: string;
  title: string;
  summary: string;
  detail: string;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  conditionSummary: string;
  requiredApp: boolean;
  requiredMembership: boolean;
  requiredPurchase: boolean;
  membershipGrade: string;
  usagePeriodDescription: string;
  availableFrom: string;
  availableTo: string;
  caution: string;
  verificationStatus: VerificationStatus;
  lastVerifiedAt: string;
  isActive: boolean;
};

type StatusFormState = {
  verificationStatus: VerificationStatus;
  lastVerifiedAt: string;
  memo: string;
};

const emptyForm: BenefitFormState = {
  brandId: "",
  title: "",
  summary: "",
  detail: "",
  benefitType: "COUPON",
  occasionType: "BIRTHDAY",
  birthdayTimingType: "UNKNOWN",
  conditionSummary: "",
  requiredApp: false,
  requiredMembership: false,
  requiredPurchase: false,
  membershipGrade: "",
  usagePeriodDescription: "",
  availableFrom: "",
  availableTo: "",
  caution: "",
  verificationStatus: "DRAFT",
  lastVerifiedAt: "",
  isActive: true,
};

const statusBadgeClass: Record<VerificationStatus, string> = {
  DRAFT: "bg-neutral-100 text-neutral-700",
  NEEDS_CHECK: "bg-amber-50 text-amber-700",
  VERIFIED: "bg-blue-50 text-blue-700",
  PUBLISHED: "bg-green-50 text-green-700",
  EXPIRED: "bg-red-50 text-red-700",
  HIDDEN: "bg-neutral-100 text-neutral-700",
};

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toRequest(form: BenefitFormState): AdminBenefitCreateRequest {
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
    requiredMembership: form.requiredMembership,
    requiredPurchase: form.requiredPurchase,
    membershipGrade: normalizeOptional(form.membershipGrade),
    usagePeriodDescription: normalizeOptional(form.usagePeriodDescription),
    availableFrom: normalizeOptional(form.availableFrom),
    availableTo: normalizeOptional(form.availableTo),
    caution: normalizeOptional(form.caution),
    verificationStatus: form.verificationStatus,
    lastVerifiedAt: normalizeOptional(form.lastVerifiedAt),
    isActive: form.isActive,
  };
}

function toFormState(benefit: AdminBenefit): BenefitFormState {
  return {
    brandId: String(benefit.brandId),
    title: benefit.title,
    summary: benefit.summary,
    detail: benefit.detail ?? "",
    benefitType: benefit.benefitType,
    occasionType: benefit.occasionType,
    birthdayTimingType: benefit.birthdayTimingType,
    conditionSummary: benefit.conditionSummary ?? "",
    requiredApp: benefit.requiredApp,
    requiredMembership: benefit.requiredMembership,
    requiredPurchase: benefit.requiredPurchase,
    membershipGrade: benefit.membershipGrade ?? "",
    usagePeriodDescription: benefit.usagePeriodDescription ?? "",
    availableFrom: benefit.availableFrom ?? "",
    availableTo: benefit.availableTo ?? "",
    caution: benefit.caution ?? "",
    verificationStatus: benefit.verificationStatus,
    lastVerifiedAt: benefit.lastVerifiedAt ?? "",
    isActive: benefit.isActive,
  };
}

function buildStatusForm(benefit: Pick<AdminBenefitSummary, "verificationStatus" | "lastVerifiedAt">): StatusFormState {
  return {
    verificationStatus: benefit.verificationStatus,
    lastVerifiedAt: benefit.lastVerifiedAt ?? "",
    memo: "",
  };
}

export function AdminBenefitsPanel() {
  const [benefits, setBenefits] = useState<AdminBenefitSummary[]>([]);
  const [brands, setBrands] = useState<AdminBrand[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebouncedValue(keyword, 300);
  const [brandSlug, setBrandSlug] = useState("ALL");
  const [categorySlug, setCategorySlug] = useState("ALL");
  const [verificationStatus, setVerificationStatus] = useState<VerificationStatus | "ALL">("ALL");
  const [benefitType, setBenefitType] = useState<BenefitType | "ALL">("ALL");
  const [birthdayTimingType, setBirthdayTimingType] = useState<BirthdayTimingType | "ALL">("ALL");
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("ALL");
  const [visibilityFilter, setVisibilityFilter] = useState<VisibilityFilter>("ALL");
  const [form, setForm] = useState<BenefitFormState>(emptyForm);
  const [editingBenefit, setEditingBenefit] = useState<AdminBenefit | null>(null);
  const [statusForms, setStatusForms] = useState<Record<number, StatusFormState>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [statusSavingId, setStatusSavingId] = useState<number | null>(null);
  const [activeSavingId, setActiveSavingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      keyword: debouncedKeyword.trim() || undefined,
      brandSlug: brandSlug === "ALL" ? undefined : brandSlug,
      categorySlug: categorySlug === "ALL" ? undefined : categorySlug,
      verificationStatus:
        visibilityFilter === "PUBLISHED_ACTIVE"
          ? "PUBLISHED"
          : visibilityFilter === "VERIFIED"
            ? "VERIFIED"
            : verificationStatus === "ALL"
              ? undefined
              : verificationStatus,
      benefitType: benefitType === "ALL" ? undefined : benefitType,
      birthdayTimingType: birthdayTimingType === "ALL" ? undefined : birthdayTimingType,
      isActive:
        visibilityFilter === "PUBLISHED_ACTIVE"
          ? true
          : visibilityFilter === "INACTIVE"
            ? false
            : activeFilter === "ALL"
              ? undefined
              : activeFilter === "ACTIVE",
    }),
    [activeFilter, benefitType, birthdayTimingType, brandSlug, categorySlug, debouncedKeyword, verificationStatus, visibilityFilter]
  );

  async function loadBenefits() {
    setLoading(true);
    setError(null);
    try {
      const data = await getAdminBenefits(params);
      setBenefits(data);
      setStatusForms(Object.fromEntries(data.map((benefit) => [benefit.id, buildStatusForm(benefit)])));
    } catch {
      setError("혜택 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    async function loadSelectData() {
      try {
        const [brandData, categoryData] = await Promise.all([getAdminBrands(), getCategories()]);
        setBrands(brandData);
        setCategories(categoryData);
      } catch {
        setError("혜택 목록을 불러오지 못했습니다.");
      }
    }

    loadSelectData();
  }, []);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getAdminBenefits(params);
        if (!active) {
          return;
        }
        setBenefits(data);
        setStatusForms(Object.fromEntries(data.map((benefit) => [benefit.id, buildStatusForm(benefit)])));
      } catch {
        if (active) {
          setError("혜택 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      active = false;
    };
  }, [params]);

  function updateForm(next: Partial<BenefitFormState>) {
    setForm((current) => ({ ...current, ...next }));
  }

  function resetForm() {
    setEditingBenefit(null);
    setForm(emptyForm);
  }

  async function startEdit(benefit: AdminBenefitSummary) {
    setMessage(null);
    setError(null);
    try {
      const detail = await getAdminBenefit(benefit.id);
      setEditingBenefit(detail);
      setForm(toFormState(detail));
    } catch {
      setError("혜택 상세 정보를 불러오지 못했습니다.");
    }
  }

  function validateForm() {
    if (!form.brandId || !form.title.trim() || !form.summary.trim()) {
      return "브랜드, 혜택명, 요약은 필수입니다.";
    }
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationMessage = validateForm();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      if (editingBenefit) {
        await updateAdminBenefit(editingBenefit.id, toRequest(form));
        setMessage("혜택 정보가 수정되었습니다.");
      } else {
        await createAdminBenefit(toRequest(form));
        setMessage("혜택이 등록되었습니다.");
      }
      resetForm();
      await loadBenefits();
    } catch {
      setError("혜택 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  function updateStatusForm(benefitId: number, next: Partial<StatusFormState>) {
    setStatusForms((current) => ({
      ...current,
      [benefitId]: { ...current[benefitId], ...next },
    }));
  }

  async function handleStatusSubmit(event: FormEvent<HTMLFormElement>, benefit: AdminBenefitSummary) {
    event.preventDefault();
    const current = statusForms[benefit.id] ?? buildStatusForm(benefit);
    setStatusSavingId(benefit.id);
    setMessage(null);
    setError(null);

    try {
      await updateAdminBenefitStatus(benefit.id, {
        verificationStatus: current.verificationStatus,
        lastVerifiedAt: normalizeOptional(current.lastVerifiedAt),
        memo: normalizeOptional(current.memo),
      });
      await loadBenefits();
      setMessage("검수 상태가 변경되었습니다.");
    } catch {
      setError("혜택 저장에 실패했습니다.");
    } finally {
      setStatusSavingId(null);
    }
  }

  async function handleActiveToggle(benefit: AdminBenefitSummary) {
    setActiveSavingId(benefit.id);
    setMessage(null);
    setError(null);

    try {
      await updateAdminBenefitActive(benefit.id, { isActive: !benefit.isActive });
      await loadBenefits();
      setMessage("혜택 노출 상태가 변경되었습니다.");
    } catch {
      setError("혜택 저장에 실패했습니다.");
    } finally {
      setActiveSavingId(null);
    }
  }

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-white p-4">
        <div>
          <h1 className="text-xl font-bold text-neutral-950">공개 혜택 관리</h1>
          <p className="mt-1 text-sm text-neutral-500">수동 등록, 공개 전환, 기존 혜택 수정을 관리합니다.</p>
        </div>
        <Link className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white" href="/admin/benefits/new">
          수동 혜택 등록
        </Link>
      </div>
      <div className="rounded-lg border border-border bg-white p-4">
        <div className="grid gap-3 lg:grid-cols-4">
          <FilterInput label="검색어" value={keyword} onChange={setKeyword} />
          <FilterSelect label="운영 보기" value={visibilityFilter} onChange={(value) => setVisibilityFilter(value as VisibilityFilter)}>
            <option value="ALL">전체</option>
            <option value="PUBLISHED_ACTIVE">공개 중</option>
            <option value="VERIFIED">검증 완료</option>
            <option value="INACTIVE">비활성</option>
          </FilterSelect>
          <FilterSelect label="브랜드" value={brandSlug} onChange={setBrandSlug}>
            <option value="ALL">전체</option>
            {brands.map((brand) => (
              <option key={brand.slug} value={brand.slug}>
                {brand.name}
              </option>
            ))}
          </FilterSelect>
          <FilterSelect label="카테고리" value={categorySlug} onChange={setCategorySlug}>
            <option value="ALL">전체</option>
            {categories.map((category) => (
              <option key={category.slug} value={category.slug}>
                {category.name}
              </option>
            ))}
          </FilterSelect>
          <FilterSelect label="검수 상태" value={verificationStatus} onChange={(value) => setVerificationStatus(value as VerificationStatus | "ALL")}>
            <option value="ALL">전체</option>
            {VERIFICATION_STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>
                {VERIFICATION_STATUS_LABELS[status]}
              </option>
            ))}
          </FilterSelect>
          <FilterSelect label="혜택 유형" value={benefitType} onChange={(value) => setBenefitType(value as BenefitType | "ALL")}>
            <option value="ALL">전체</option>
            {BENEFIT_TYPE_OPTIONS.map((type) => (
              <option key={type} value={type}>
                {BENEFIT_TYPE_LABELS[type]}
              </option>
            ))}
          </FilterSelect>
          <FilterSelect label="생일 혜택 기간" value={birthdayTimingType} onChange={(value) => setBirthdayTimingType(value as BirthdayTimingType | "ALL")}>
            <option value="ALL">전체</option>
            {BIRTHDAY_TIMING_OPTIONS.map((type) => (
              <option key={type} value={type}>
                {BIRTHDAY_TIMING_LABELS[type]}
              </option>
            ))}
          </FilterSelect>
          <FilterSelect label="활성 상태" value={activeFilter} onChange={(value) => setActiveFilter(value as ActiveFilter)}>
            <option value="ALL">전체</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </FilterSelect>
        </div>
      </div>

      {message ? <Notice tone="success">{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}
      {keyword !== debouncedKeyword ? <Notice tone="default">검색어를 적용하는 중입니다.</Notice> : null}

      <div className="grid gap-5 2xl:grid-cols-[420px_1fr]">
        <BenefitForm
          brands={brands}
          editingBenefit={editingBenefit}
          form={form}
          saving={saving}
          onCancel={resetForm}
          onChange={updateForm}
          onSubmit={handleSubmit}
        />

        <div className="space-y-4">
          {loading ? <EmptyBox>혜택을 불러오는 중입니다.</EmptyBox> : null}
          {!loading && benefits.length === 0 && !error ? <EmptyBox>아직 등록된 혜택이 없습니다.</EmptyBox> : null}

          {benefits.map((benefit) => (
            <BenefitCard
              key={benefit.id}
              benefit={benefit}
              statusForm={statusForms[benefit.id] ?? buildStatusForm(benefit)}
              statusSaving={statusSavingId === benefit.id}
              activeSaving={activeSavingId === benefit.id}
              onActiveToggle={() => handleActiveToggle(benefit)}
              onEdit={() => startEdit(benefit)}
              onStatusChange={(next) => updateStatusForm(benefit.id, next)}
              onStatusSubmit={(event) => handleStatusSubmit(event, benefit)}
            />
          ))}

          <p className="rounded-lg border border-border bg-white p-4 text-xs leading-5 text-neutral-600">
            PUBLISHED 상태이면서 활성화된 혜택만 사용자 화면에 노출됩니다.
          </p>
        </div>
      </div>
    </section>
  );
}

function BenefitForm({
  brands,
  editingBenefit,
  form,
  saving,
  onCancel,
  onChange,
  onSubmit,
}: {
  brands: AdminBrand[];
  editingBenefit: AdminBenefit | null;
  form: BenefitFormState;
  saving: boolean;
  onCancel: () => void;
  onChange: (next: Partial<BenefitFormState>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <form className="space-y-4 rounded-lg border border-border bg-white p-5" onSubmit={onSubmit}>
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">{editingBenefit ? "혜택 수정" : "혜택 등록"}</h2>
        {editingBenefit ? (
          <button className="text-sm font-semibold text-accent" type="button" onClick={onCancel}>
            새로 등록
          </button>
        ) : null}
      </div>
      <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
        공식 출처로 확인되지 않은 혜택은 PUBLISHED 상태로 게시하지 않는 것을 권장합니다.
      </p>

      <FilterSelect label="브랜드" value={form.brandId} onChange={(value) => onChange({ brandId: value })} required>
        <option value="">선택</option>
        {brands.map((brand) => (
          <option key={brand.id} value={brand.id}>
            {brand.name}
          </option>
        ))}
      </FilterSelect>
      <TextInput label="혜택명" value={form.title} onChange={(value) => onChange({ title: value })} required />
      <TextArea label="요약" value={form.summary} onChange={(value) => onChange({ summary: value })} required />
      <TextArea label="상세 설명" value={form.detail} onChange={(value) => onChange({ detail: value })} />
      <div className="grid gap-3 sm:grid-cols-2">
        <EnumSelect label="혜택 유형" value={form.benefitType} values={BENEFIT_TYPE_OPTIONS} labels={BENEFIT_TYPE_LABELS} onChange={(value) => onChange({ benefitType: value as BenefitType })} />
        <EnumSelect label="대상 occasionType" value={form.occasionType} values={OCCASION_TYPE_OPTIONS} labels={OCCASION_TYPE_LABELS} onChange={(value) => onChange({ occasionType: value as OccasionType })} />
        <EnumSelect label="생일 기간 유형" value={form.birthdayTimingType} values={BIRTHDAY_TIMING_OPTIONS} labels={BIRTHDAY_TIMING_LABELS} onChange={(value) => onChange({ birthdayTimingType: value as BirthdayTimingType })} />
        <EnumSelect label="검수 상태" value={form.verificationStatus} values={VERIFICATION_STATUS_OPTIONS} labels={VERIFICATION_STATUS_LABELS} onChange={(value) => onChange({ verificationStatus: value as VerificationStatus })} />
      </div>
      <TextInput label="조건 요약" value={form.conditionSummary} onChange={(value) => onChange({ conditionSummary: value })} />
      <div className="grid gap-2 sm:grid-cols-3">
        <Checkbox label="앱 필요" checked={form.requiredApp} onChange={(value) => onChange({ requiredApp: value })} />
        <Checkbox label="멤버십 필요" checked={form.requiredMembership} onChange={(value) => onChange({ requiredMembership: value })} />
        <Checkbox label="구매 조건" checked={form.requiredPurchase} onChange={(value) => onChange({ requiredPurchase: value })} />
      </div>
      <TextInput label="멤버십 등급" value={form.membershipGrade} onChange={(value) => onChange({ membershipGrade: value })} />
      <TextInput label="사용 가능 기간 설명" value={form.usagePeriodDescription} onChange={(value) => onChange({ usagePeriodDescription: value })} />
      <div className="grid gap-3 sm:grid-cols-2">
        <TextInput label="시작일" type="date" value={form.availableFrom} onChange={(value) => onChange({ availableFrom: value })} />
        <TextInput label="종료일" type="date" value={form.availableTo} onChange={(value) => onChange({ availableTo: value })} />
        <TextInput label="최근 확인일" type="date" value={form.lastVerifiedAt} onChange={(value) => onChange({ lastVerifiedAt: value })} />
      </div>
      <TextArea label="주의사항" value={form.caution} onChange={(value) => onChange({ caution: value })} />
      <Checkbox label="활성 상태로 노출" checked={form.isActive} onChange={(value) => onChange({ isActive: value })} />
      <button className="h-11 w-full rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={saving}>
        {saving ? "저장 중" : editingBenefit ? "수정 저장" : "혜택 등록"}
      </button>
    </form>
  );
}

function BenefitCard({
  benefit,
  statusForm,
  statusSaving,
  activeSaving,
  onActiveToggle,
  onEdit,
  onStatusChange,
  onStatusSubmit,
}: {
  benefit: AdminBenefitSummary;
  statusForm: StatusFormState;
  statusSaving: boolean;
  activeSaving: boolean;
  onActiveToggle: () => void;
  onEdit: () => void;
  onStatusChange: (next: Partial<StatusFormState>) => void;
  onStatusSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <article className="rounded-lg border border-border bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold">{benefit.title}</h2>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass[benefit.verificationStatus]}`}>
              {VERIFICATION_STATUS_LABELS[benefit.verificationStatus]}
            </span>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${benefit.isActive ? "bg-green-50 text-green-700" : "bg-neutral-100 text-neutral-700"}`}>
              {benefit.isActive ? "활성" : "비활성"}
            </span>
          </div>
          <p className="mt-1 text-sm text-neutral-500">
            {benefit.brandName}
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold"
            href={`/brands/${benefit.brandSlug}`}
            target="_blank"
          >
            사용자 화면 보기
          </Link>
          <Link
            className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold"
            href={`/admin/benefits/${benefit.id}`}
          >
            수정하기
          </Link>
          <button className="h-9 rounded-lg border border-border px-3 text-sm font-semibold" type="button" onClick={onEdit}>
            빠른 수정
          </button>
          <button className="h-9 rounded-lg border border-border px-3 text-sm font-semibold disabled:opacity-60" type="button" onClick={onActiveToggle} disabled={activeSaving}>
            공개 상태 변경
          </button>
        </div>
      </div>
      <p className="mt-4 text-sm leading-6 text-neutral-700">{benefit.summary}</p>
      <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
        <Info label="혜택 유형" value={BENEFIT_TYPE_LABELS[benefit.benefitType]} />
        <Info label="적용 시점" value={OCCASION_TYPE_LABELS[benefit.applicableTiming]} />
        <Info label="최근 확인일" value={benefit.lastVerifiedAt ?? "-"} />
        <Info label="대표 혜택 개수" value={String(benefit.detailItemCount)} />
        <Info label="공식 출처 개수" value={String(benefit.sourceCount)} />
        <Info label="태그 개수" value={String(benefit.tagCount)} />
      </dl>

      <form className="mt-5 grid gap-3 rounded-lg border border-border p-4 lg:grid-cols-[160px_160px_1fr_100px]" onSubmit={onStatusSubmit}>
        <EnumSelect label="검수 상태" value={statusForm.verificationStatus} values={VERIFICATION_STATUS_OPTIONS} labels={VERIFICATION_STATUS_LABELS} onChange={(value) => onStatusChange({ verificationStatus: value as VerificationStatus })} />
        <TextInput label="최근 확인일" type="date" value={statusForm.lastVerifiedAt} onChange={(value) => onStatusChange({ lastVerifiedAt: value })} />
        <TextInput label="메모" value={statusForm.memo} onChange={(value) => onStatusChange({ memo: value })} />
        <button className="mt-7 h-10 rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={statusSaving}>
          저장
        </button>
      </form>
    </article>
  );
}

function FilterInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return <TextInput label={label} value={value} onChange={onChange} placeholder="브랜드명 또는 혜택명 검색" />;
}

function FilterSelect({
  children,
  label,
  required,
  value,
  onChange,
}: {
  children: ReactNode;
  label: string;
  required?: boolean;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <select className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required={required}>
        {children}
      </select>
    </label>
  );
}

function EnumSelect<T extends string>({ label, labels, value, values, onChange }: { label: string; labels: Record<T, string>; value: T; values: T[]; onChange: (value: T) => void }) {
  return (
    <FilterSelect label={label} value={value} onChange={(next) => onChange(next as T)}>
      {values.map((item) => (
        <option key={item} value={item}>
          {labels[item]}
        </option>
      ))}
    </FilterSelect>
  );
}

function TextInput({ label, onChange, placeholder, required, type = "text", value }: { label: string; onChange: (value: string) => void; placeholder?: string; required?: boolean; type?: string; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <input className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} required={required} type={type} />
    </label>
  );
}

function TextArea({ label, onChange, required, value }: { label: string; onChange: (value: string) => void; required?: boolean; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <textarea className="min-h-24 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm leading-6" value={value} onChange={(event) => onChange(event.target.value)} required={required} />
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
    <div>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 text-neutral-800">{value}</dd>
    </div>
  );
}

function EmptyBox({ children }: { children: ReactNode }) {
  return <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">{children}</div>;
}

function Notice({ children, tone }: { children: ReactNode; tone: "success" | "error" | "default" }) {
  const className =
    tone === "success"
      ? "border-green-200 bg-green-50 text-green-700"
      : tone === "error"
        ? "border-red-200 bg-red-50 text-red-700"
        : "border-neutral-200 bg-neutral-50 text-neutral-600";
  return <div className={`rounded-lg border p-4 text-sm ${className}`}>{children}</div>;
}
