"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  approveBenefitCandidate,
  getBenefitCandidate,
  getSourceWatches,
  updateBenefitCandidateStatus,
} from "@/lib/api/adminApi";
import {
  BIRTHDAY_TIMING_OPTIONS,
  BENEFIT_TYPE_OPTIONS,
  OCCASION_TYPE_OPTIONS,
} from "@/lib/benefitLabels";
import type {
  BenefitCandidate,
  BenefitCandidateApproveRequest,
  BenefitCandidateApproveResponse,
} from "@/types/benefitCandidate";
import type {
  BenefitType,
  BirthdayTimingType,
  OccasionType,
} from "@/types/adminBenefit";
import type { SourceWatch } from "@/types/sourceWatch";

type ApproveFormState = {
  title: string;
  summary: string;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  birthdayTimingDescription: string;
  requiresApp: boolean;
  requiresSignup: boolean;
  requiresMembership: boolean;
  minimumPurchaseDescription: string;
  usageCondition: string;
  adminMemo: string;
};

function toApproveForm(candidate: BenefitCandidate): ApproveFormState {
  return {
    title: candidate.title,
    summary: candidate.summary,
    benefitType: candidate.benefitType,
    occasionType: candidate.occasionType ?? "BIRTHDAY",
    birthdayTimingType: candidate.birthdayTimingType,
    birthdayTimingDescription: "",
    requiresApp: candidate.requiresApp,
    requiresSignup: candidate.requiresSignup,
    requiresMembership: candidate.requiresMembership,
    minimumPurchaseDescription: "",
    usageCondition: candidate.evidenceText,
    adminMemo: "",
  };
}

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toApproveRequest(form: ApproveFormState): BenefitCandidateApproveRequest {
  return {
    title: form.title.trim(),
    summary: form.summary.trim(),
    benefitType: form.benefitType,
    occasionType: form.occasionType,
    birthdayTimingType: form.birthdayTimingType,
    birthdayTimingDescription: normalizeOptional(form.birthdayTimingDescription),
    requiresApp: form.requiresApp,
    requiresSignup: form.requiresSignup,
    requiresMembership: form.requiresMembership,
    minimumPurchaseDescription: normalizeOptional(form.minimumPurchaseDescription),
    usageCondition: normalizeOptional(form.usageCondition),
    adminMemo: normalizeOptional(form.adminMemo),
  };
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function getErrorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message ?? fallback;
  }
  return fallback;
}

export function AdminBenefitCandidateDetailPanel({ candidateId }: { candidateId: number }) {
  const [candidate, setCandidate] = useState<BenefitCandidate | null>(null);
  const [sourceWatches, setSourceWatches] = useState<SourceWatch[]>([]);
  const [approveForm, setApproveForm] = useState<ApproveFormState | null>(null);
  const [statusMemo, setStatusMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingStatus, setSavingStatus] = useState(false);
  const [approving, setApproving] = useState(false);
  const [approveResult, setApproveResult] = useState<BenefitCandidateApproveResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadAll() {
    setLoading(true);
    setError(null);
    try {
      const [candidateData, sourceWatchData] = await Promise.all([
        getBenefitCandidate(candidateId),
        getSourceWatches(),
      ]);
      setCandidate(candidateData);
      setSourceWatches(sourceWatchData);
      setApproveForm(toApproveForm(candidateData));
    } catch (loadError) {
      setError(getErrorMessage(loadError, "혜택 후보 상세를 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, [candidateId]);

  const sourceWatch = useMemo(
    () => sourceWatches.find((item) => item.id === candidate?.sourceWatchId) ?? null,
    [candidate?.sourceWatchId, sourceWatches]
  );

  const approveDisabled = !candidate || candidate.status === "REJECTED" || candidate.approvedBenefitId !== null;

  async function reloadCandidate() {
    const next = await getBenefitCandidate(candidateId);
    setCandidate(next);
    return next;
  }

  async function handleStatus(status: "NEEDS_REVIEW" | "REJECTED") {
    setSavingStatus(true);
    setMessage(null);
    setError(null);
    try {
      const updated = await updateBenefitCandidateStatus(candidateId, {
        status,
        reviewMemo: normalizeOptional(statusMemo),
      });
      setCandidate(updated);
      setMessage(`후보 상태가 ${status}로 변경되었습니다.`);
    } catch (statusError) {
      setError(getErrorMessage(statusError, "후보 상태 변경에 실패했습니다."));
    } finally {
      setSavingStatus(false);
    }
  }

  async function handleApprove(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!approveForm) {
      return;
    }
    if (!approveForm.title.trim() || !approveForm.summary.trim()) {
      setError("승인할 Benefit의 제목과 요약은 필수입니다.");
      return;
    }

    setApproving(true);
    setMessage(null);
    setError(null);
    try {
      const result = await approveBenefitCandidate(candidateId, toApproveRequest(approveForm));
      setApproveResult(result);
      setMessage(`Benefit 생성 완료: #${result.benefitId}`);
      await reloadCandidate();
    } catch (approveError) {
      setError(getErrorMessage(approveError, "Candidate 승인에 실패했습니다."));
    } finally {
      setApproving(false);
    }
  }

  if (loading) {
    return <EmptyBox>혜택 후보 상세를 불러오는 중입니다.</EmptyBox>;
  }

  if (!candidate || !approveForm) {
    return (
      <section className="space-y-4">
        <BackLink />
        <Notice tone="error">{error ?? "혜택 후보 상세를 불러오지 못했습니다."}</Notice>
      </section>
    );
  }

  const benefitId = candidate.approvedBenefitId ?? approveResult?.benefitId ?? null;

  return (
    <section className="space-y-5">
      <BackLink />
      {message ? <Notice tone="success">{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}

      <section className="rounded-lg border border-border bg-white p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold">{candidate.title}</h2>
            <p className="mt-1 text-sm text-neutral-500">{candidate.brandName} · {candidate.status}</p>
          </div>
          {benefitId ? (
            <Link className="rounded-lg bg-accent px-4 py-2 text-sm font-semibold text-white" href={`/admin/benefits/${benefitId}`}>
              생성된 혜택 관리로 이동
            </Link>
          ) : null}
        </div>
        <dl className="mt-5 grid gap-3 text-sm md:grid-cols-2">
          <Info label="SourceWatch 제목" value={sourceWatch?.title ?? `#${candidate.sourceWatchId}`} />
          <Info label="SourceWatch URL" value={sourceWatch?.url ?? "-"} />
          <Info label="summary" value={candidate.summary} wide />
          <Info label="evidenceText" value={candidate.evidenceText} wide />
          <Info label="benefitType" value={candidate.benefitType} />
          <Info label="occasionType" value={candidate.occasionType} />
          <Info label="birthdayTimingType" value={candidate.birthdayTimingType} />
          <Info label="requiresApp" value={String(candidate.requiresApp)} />
          <Info label="requiresSignup" value={String(candidate.requiresSignup)} />
          <Info label="requiresMembership" value={String(candidate.requiresMembership)} />
          <Info label="confidence" value={Number(candidate.confidence).toFixed(2)} />
          <Info label="approvedBenefitId" value={candidate.approvedBenefitId ? String(candidate.approvedBenefitId) : "-"} />
          <Info label="approvedAt" value={formatDateTime(candidate.approvedAt)} />
        </dl>
      </section>

      <section className="rounded-lg border border-border bg-white p-5">
        <h2 className="text-lg font-semibold">상태 변경</h2>
        <p className="mt-2 text-sm leading-6 text-neutral-600">
          APPROVED는 상태 변경 API가 아니라 승인 API로만 처리합니다.
        </p>
        <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto_auto]">
          <TextInput label="검수 메모" value={statusMemo} onChange={setStatusMemo} />
          <button className="mt-7 h-11 rounded-lg border border-border px-4 text-sm font-semibold disabled:opacity-60" type="button" disabled={savingStatus || candidate.status === "APPROVED"} onClick={() => handleStatus("NEEDS_REVIEW")}>
            NEEDS_REVIEW
          </button>
          <button className="mt-7 h-11 rounded-lg border border-border px-4 text-sm font-semibold disabled:opacity-60" type="button" disabled={savingStatus || candidate.status === "APPROVED"} onClick={() => handleStatus("REJECTED")}>
            REJECTED
          </button>
        </div>
      </section>

      <form className="space-y-4 rounded-lg border border-border bg-white p-5" onSubmit={handleApprove}>
        <div>
          <h2 className="text-lg font-semibold">Benefit 승인 폼</h2>
          <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm leading-6 text-amber-800">
            승인된 후보는 VERIFIED 상태의 혜택으로 생성됩니다. PUBLISHED 전환 전까지 사용자 화면에는 노출되지 않습니다.
          </p>
        </div>
        <TextInput label="title" value={approveForm.title} onChange={(title) => setApproveForm((current) => current && { ...current, title })} required />
        <TextArea label="summary" value={approveForm.summary} onChange={(summary) => setApproveForm((current) => current && { ...current, summary })} required />
        <div className="grid gap-3 md:grid-cols-3">
          <Select label="benefitType" value={approveForm.benefitType} onChange={(benefitType) => setApproveForm((current) => current && { ...current, benefitType: benefitType as BenefitType })}>
            {BENEFIT_TYPE_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
          </Select>
          <Select label="occasionType" value={approveForm.occasionType} onChange={(occasionType) => setApproveForm((current) => current && { ...current, occasionType: occasionType as OccasionType })}>
            {OCCASION_TYPE_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
          </Select>
          <Select label="birthdayTimingType" value={approveForm.birthdayTimingType} onChange={(birthdayTimingType) => setApproveForm((current) => current && { ...current, birthdayTimingType: birthdayTimingType as BirthdayTimingType })}>
            {BIRTHDAY_TIMING_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
          </Select>
        </div>
        <TextInput label="birthdayTimingDescription" value={approveForm.birthdayTimingDescription} onChange={(birthdayTimingDescription) => setApproveForm((current) => current && { ...current, birthdayTimingDescription })} />
        <div className="grid gap-3 md:grid-cols-3">
          <Checkbox label="requiresApp" checked={approveForm.requiresApp} onChange={(requiresApp) => setApproveForm((current) => current && { ...current, requiresApp })} />
          <Checkbox label="requiresSignup" checked={approveForm.requiresSignup} onChange={(requiresSignup) => setApproveForm((current) => current && { ...current, requiresSignup })} />
          <Checkbox label="requiresMembership" checked={approveForm.requiresMembership} onChange={(requiresMembership) => setApproveForm((current) => current && { ...current, requiresMembership })} />
        </div>
        <TextInput label="minimumPurchaseDescription" value={approveForm.minimumPurchaseDescription} onChange={(minimumPurchaseDescription) => setApproveForm((current) => current && { ...current, minimumPurchaseDescription })} />
        <TextArea label="usageCondition" value={approveForm.usageCondition} onChange={(usageCondition) => setApproveForm((current) => current && { ...current, usageCondition })} />
        <TextArea label="adminMemo" value={approveForm.adminMemo} onChange={(adminMemo) => setApproveForm((current) => current && { ...current, adminMemo })} />
        <button className="h-11 w-full rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={approveDisabled || approving}>
          {approving ? "승인 중" : "Benefit으로 승인"}
        </button>
        {approveDisabled ? (
          <p className="text-sm text-neutral-500">
            이미 승인되었거나 반려된 후보는 승인할 수 없습니다.
          </p>
        ) : null}
      </form>
    </section>
  );
}

function BackLink() {
  return (
    <Link className="inline-flex text-sm font-semibold text-accent" href="/admin/benefit-candidates">
      혜택 후보 목록으로 돌아가기
    </Link>
  );
}

function Info({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={wide ? "md:col-span-2" : undefined}>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 whitespace-pre-wrap break-words text-neutral-800">{value}</dd>
    </div>
  );
}

function Select({ children, label, onChange, value }: { children: React.ReactNode; label: string; onChange: (value: string) => void; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <select className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}>
        {children}
      </select>
    </label>
  );
}

function TextInput({ label, onChange, required, value }: { label: string; onChange: (value: string) => void; required?: boolean; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <input className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required={required} />
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

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">{children}</div>;
}

function Notice({ children, tone }: { children: React.ReactNode; tone: "success" | "error" }) {
  const className = tone === "success" ? "border-green-200 bg-green-50 text-green-700" : "border-red-200 bg-red-50 text-red-700";
  return <div className={`rounded-lg border p-4 text-sm ${className}`}>{children}</div>;
}
