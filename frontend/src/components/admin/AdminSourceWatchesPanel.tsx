"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import {
  collectSourceWatch,
  createSourceWatch,
  getAdminBrands,
  getSourceWatchCollectionRuns,
  getSourceWatches,
  regenerateSourceWatchCandidates,
  updateSourceWatch,
  updateSourceWatchActive,
  updateSourceWatchTermsCheck,
} from "@/lib/api/adminApi";
import { COLLECTION_FAILURE_REASON_LABELS, COLLECTION_STATUS_CLASS, COLLECTION_STATUS_LABELS } from "@/lib/adminLabels";
import { SOURCE_TYPE_LABELS, SOURCE_TYPE_OPTIONS } from "@/lib/sourceLabels";
import type { SourceType } from "@/types/adminBenefitSource";
import type { AdminBrand } from "@/types/adminBrand";
import type { SourceWatchCollectionRunHistory } from "@/types/collectionRun";
import type {
  CollectionMethod,
  CollectionPermissionStatus,
  RecentCollectionRunSummary,
  RobotsCheckStatus,
  SourceWatch,
  SourceWatchCollectResponse,
  SourceWatchRegenerateCandidatesResponse,
  SourceWatchTermsCheckRequest,
  TermsCheckStatus,
} from "@/types/sourceWatch";

type FormState = {
  brandId: string;
  sourceType: SourceType;
  title: string;
  url: string;
  isActive: boolean;
  loginRequired: boolean;
  robotsCheckStatus: RobotsCheckStatus;
  termsCheckStatus: TermsCheckStatus;
  collectionMethod: CollectionMethod;
  collectionPermissionStatus: CollectionPermissionStatus | "";
  policyCheckNote: string;
  manualVerificationNote: string;
};

type TermsFormState = {
  termsCheckStatus: TermsCheckStatus;
  termsUrl: string;
  termsCheckedAt: string;
  termsMemo: string;
};

const emptyForm: FormState = {
  brandId: "",
  sourceType: "OFFICIAL_HOME",
  title: "",
  url: "",
  isActive: true,
  loginRequired: false,
  robotsCheckStatus: "UNKNOWN",
  termsCheckStatus: "NOT_CHECKED",
  collectionMethod: "UNKNOWN",
  collectionPermissionStatus: "",
  policyCheckNote: "",
  manualVerificationNote: "",
};

function toForm(sourceWatch: SourceWatch): FormState {
  return {
    brandId: sourceWatch.brandId ? String(sourceWatch.brandId) : "",
    sourceType: sourceWatch.sourceType ?? "OFFICIAL_HOME",
    title: sourceWatch.title ?? "",
    url: sourceWatch.url ?? "",
    isActive: Boolean(sourceWatch.isActive),
    loginRequired: Boolean(sourceWatch.loginRequired),
    robotsCheckStatus: sourceWatch.robotsCheckStatus ?? "UNKNOWN",
    termsCheckStatus: sourceWatch.termsCheckStatus ?? "NOT_CHECKED",
    collectionMethod: sourceWatch.collectionMethod ?? "UNKNOWN",
    collectionPermissionStatus:
      sourceWatch.collectionPermissionStatus === "MANUAL_REVIEW_ONLY"
        ? "MANUAL_REVIEW_ONLY"
        : "",
    policyCheckNote: sourceWatch.policyCheckNote ?? "",
    manualVerificationNote: sourceWatch.manualVerificationNote ?? "",
  };
}

function toTermsForm(sourceWatch: SourceWatch): TermsFormState {
  return {
    termsCheckStatus: sourceWatch.termsCheckStatus ?? "NOT_CHECKED",
    termsUrl: sourceWatch.termsUrl ?? "",
    termsCheckedAt: sourceWatch.termsCheckedAt ?? "",
    termsMemo: sourceWatch.termsMemo ?? "",
  };
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message ?? fallback;
  }
  return fallback;
}

const skippedFailureReasons = new Set([
  "SOURCE_WATCH_INACTIVE",
  "RATE_LIMITED_BY_DOMAIN",
  "COLLECTION_ALREADY_RUNNING",
  "ROBOTS_TXT_DISALLOWED",
  "ROBOTS_TXT_FETCH_FAILED",
  "ROBOTS_TXT_PARSE_FAILED",
  "TERMS_RESTRICTION_FOUND",
  "TERMS_BLOCKED",
  "TERMS_NOT_CHECKED",
  "COLLECTION_PERMISSION_NOT_APPROVED",
  "LOGIN_REQUIRED_SOURCE",
  "LOGIN_REQUIRED",
  "POLICY_NEEDS_REVIEW",
  "UNKNOWN_POLICY_NEEDS_REVIEW",
]);

const permissionLabels: Record<CollectionPermissionStatus, string> = {
  ALLOWED_TO_COLLECT: "자동 수집 가능",
  MANUAL_REVIEW_ONLY: "수동 검수 필요",
  BLOCKED_BY_ROBOTS: "자동 수집 불가 · robots.txt 차단",
  BLOCKED_BY_TERMS: "약관 금지",
  LOGIN_REQUIRED: "로그인 필요",
  UNKNOWN_NEEDS_REVIEW: "정책 확인 필요",
};

const permissionClasses: Record<CollectionPermissionStatus, string> = {
  ALLOWED_TO_COLLECT: "bg-green-50 text-green-700",
  MANUAL_REVIEW_ONLY: "bg-amber-50 text-amber-700",
  BLOCKED_BY_ROBOTS: "bg-red-50 text-red-700",
  BLOCKED_BY_TERMS: "bg-red-50 text-red-700",
  LOGIN_REQUIRED: "bg-neutral-100 text-neutral-700",
  UNKNOWN_NEEDS_REVIEW: "bg-amber-50 text-amber-700",
};

const termsCheckLabels: Record<TermsCheckStatus, string> = {
  NOT_CHECKED: "미확인",
  NO_RESTRICTION_FOUND: "수집 가능",
  NEEDS_REVIEW: "검토 필요",
  RESTRICTION_FOUND: "제한 있음",
  BLOCKED: "수집 금지",
};

const termsCandidateTypeLabels: Record<string, string> = {
  TERMS: "약관",
  LEGAL: "법적고지",
  PRIVACY: "개인정보처리방침",
  COPYRIGHT: "저작권",
  OTHER: "기타",
};

const termsCandidateClass: Record<string, string> = {
  TERMS: "bg-blue-50 text-blue-700",
  LEGAL: "bg-violet-50 text-violet-700",
  PRIVACY: "bg-neutral-100 text-neutral-700",
  COPYRIGHT: "bg-amber-50 text-amber-700",
  OTHER: "bg-neutral-100 text-neutral-700",
};

function isUnverifiedPolicy(sourceWatch: SourceWatch) {
  return (
    sourceWatch.collectionPermissionStatus === "UNKNOWN_NEEDS_REVIEW" &&
    (sourceWatch.termsCheckStatus === "NOT_CHECKED" || sourceWatch.termsCheckStatus === "NO_RESTRICTION_FOUND") &&
    sourceWatch.robotsCheckStatus === "UNKNOWN"
  );
}

function canCollect(sourceWatch: SourceWatch) {
  if (!sourceWatch.isActive || sourceWatch.loginRequired) {
    return false;
  }
  if (sourceWatch.collectionPermissionStatus === "ALLOWED_TO_COLLECT") {
    return true;
  }
  if (sourceWatch.collectionPermissionStatus === "BLOCKED_BY_ROBOTS" || sourceWatch.collectionPermissionStatus === "BLOCKED_BY_TERMS") {
    return false;
  }
  if (sourceWatch.collectionPermissionStatus === "LOGIN_REQUIRED" || sourceWatch.collectionPermissionStatus === "MANUAL_REVIEW_ONLY") {
    return false;
  }
  if (sourceWatch.robotsCheckStatus === "DISALLOWED") {
    return false;
  }
  if (sourceWatch.termsCheckStatus === "NEEDS_REVIEW" || sourceWatch.termsCheckStatus === "RESTRICTION_FOUND" || sourceWatch.termsCheckStatus === "BLOCKED") {
    return false;
  }
  return isUnverifiedPolicy(sourceWatch);
}

function getCollectButtonLabel(sourceWatch: SourceWatch) {
  return isUnverifiedPolicy(sourceWatch) ? "정책 확인 후 수집 실행" : "수집 실행";
}

function getPolicyGuidance(sourceWatch: SourceWatch) {
  if (isUnverifiedPolicy(sourceWatch)) {
    return "아직 정책 점검을 수행하지 않았습니다. 수집 실행을 누르면 robots.txt와 접근 가능 여부를 먼저 확인한 뒤, 허용되는 경우에만 수집을 진행합니다.";
  }
  if (sourceWatch.collectionPermissionStatus === "ALLOWED_TO_COLLECT") {
    return "자동 점검 결과 수집 가능한 출처입니다. 생성된 후보는 관리자 검수 후에만 공개됩니다.";
  }
  if (sourceWatch.collectionPermissionStatus === "UNKNOWN_NEEDS_REVIEW") {
    return "자동 점검 결과 수동 검토가 필요한 출처입니다. 정책 메모와 최근 수집 이력을 확인한 뒤 수동 혜택 등록 또는 정책 상태 수정을 진행해 주세요.";
  }
  if (sourceWatch.collectionPermissionStatus === "BLOCKED_BY_ROBOTS") {
    return "robots.txt 정책에 의해 자동 수집이 차단되었습니다. 공식 페이지를 직접 확인한 뒤 수동 혜택 등록 또는 기존 혜택 수정을 진행해 주세요.";
  }
  if (sourceWatch.collectionPermissionStatus === "LOGIN_REQUIRED") {
    return "로그인 또는 인증이 필요한 페이지로 판단되어 자동 수집을 진행하지 않습니다. 공식 페이지를 직접 확인한 뒤 수동으로 혜택을 등록해 주세요.";
  }
  return "이 출처는 자동 수집 대상이 아닙니다. 공식 페이지를 직접 확인한 뒤 최소 사실 정보 중심으로 수동 등록하거나 수정해 주세요.";
}

function isRegenerationBlocked(sourceWatch: SourceWatch) {
  return sourceWatch.collectionPermissionStatus !== "ALLOWED_TO_COLLECT";
}

function getFailureReasonLabel(failureReason: string | null) {
  if (!failureReason) {
    return null;
  }
  return COLLECTION_FAILURE_REASON_LABELS[failureReason] ?? failureReason;
}

function getCollectionOutcomeText(result: SourceWatchCollectResponse) {
  if (!result.failureReason) {
    return `수집 완료: 후보 ${result.candidateCount}개 생성, 동일 HTML 여부 ${String(result.sameAsPrevious)}`;
  }
  const label = getFailureReasonLabel(result.failureReason) ?? result.failureReason;
  return skippedFailureReasons.has(result.failureReason)
    ? `수집 건너뜀: ${label}`
    : `수집 실패: ${label}`;
}

function extractMatchedRule(errorMessage: string | null) {
  if (!errorMessage) {
    return null;
  }
  const marker = "matchedRule:";
  const index = errorMessage.indexOf(marker);
  if (index < 0) {
    return null;
  }
  return errorMessage.slice(index + marker.length).trim() || null;
}

function getRobotsSummary(run: RecentCollectionRunSummary | null) {
  if (!run) {
    return { label: "확인 이력 없음", detail: null };
  }
  if (run.failureReason === "ROBOTS_TXT_DISALLOWED") {
    return { label: "차단", detail: extractMatchedRule(run.errorMessage) };
  }
  if (run.failureReason === "ROBOTS_TXT_FETCH_FAILED") {
    return { label: "조회 실패", detail: run.errorMessage };
  }
  if (run.failureReason === "ROBOTS_TXT_PARSE_FAILED") {
    return { label: "파싱 실패", detail: run.errorMessage };
  }
  return { label: "허용 후 수집 진행", detail: null };
}

export function AdminSourceWatchesPanel() {
  const searchParams = useSearchParams();
  const highlightedSourceWatchId = Number(searchParams.get("sourceWatchId") ?? "");
  const [sourceWatches, setSourceWatches] = useState<SourceWatch[]>([]);
  const [brands, setBrands] = useState<AdminBrand[]>([]);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [editing, setEditing] = useState<SourceWatch | null>(null);
  const [showFixtures, setShowFixtures] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deactivatingFixtures, setDeactivatingFixtures] = useState(false);
  const [collectingId, setCollectingId] = useState<number | null>(null);
  const [regeneratingId, setRegeneratingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [collectResults, setCollectResults] = useState<Record<number, SourceWatchCollectResponse>>({});
  const [regenerateResults, setRegenerateResults] = useState<Record<number, SourceWatchRegenerateCandidatesResponse>>({});
  const [expandedHistoryId, setExpandedHistoryId] = useState<number | null>(null);
  const [historyLoadingId, setHistoryLoadingId] = useState<number | null>(null);
  const [historyRuns, setHistoryRuns] = useState<Record<number, SourceWatchCollectionRunHistory[]>>({});
  const [historyErrors, setHistoryErrors] = useState<Record<number, string>>({});
  const [termsForms, setTermsForms] = useState<Record<number, TermsFormState>>({});
  const [savingTermsId, setSavingTermsId] = useState<number | null>(null);

  async function loadAll() {
    setLoading(true);
    setError(null);
    try {
      const [watchData, brandData] = await Promise.all([getSourceWatches(), getAdminBrands()]);
      setSourceWatches(watchData ?? []);
      setTermsForms(Object.fromEntries((watchData ?? []).map((sourceWatch) => [sourceWatch.id, toTermsForm(sourceWatch)])));
      setBrands(brandData ?? []);
    } catch (loadError) {
      setError(getErrorMessage(loadError, "SourceWatch 목록을 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  function resetForm() {
    setEditing(null);
    setForm(emptyForm);
  }

  function startEdit(sourceWatch: SourceWatch) {
    setEditing(sourceWatch);
    setForm(toForm(sourceWatch));
    setMessage(null);
    setError(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.brandId || !form.title.trim() || !form.url.trim()) {
      setError("브랜드, 제목, URL은 필수입니다.");
      return;
    }

    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const request = {
        brandId: Number(form.brandId),
        sourceType: form.sourceType,
        title: form.title.trim(),
        url: form.url.trim(),
        isActive: form.isActive,
        loginRequired: form.loginRequired,
        robotsCheckStatus: form.robotsCheckStatus,
        termsCheckStatus: form.termsCheckStatus,
        collectionMethod: form.collectionMethod,
        collectionPermissionStatus: form.collectionPermissionStatus || undefined,
        policyCheckNote: normalizeOptional(form.policyCheckNote),
        manualVerificationNote: normalizeOptional(form.manualVerificationNote),
      };
      if (editing) {
        await updateSourceWatch(editing.id, request);
        setMessage("SourceWatch가 수정되었습니다.");
      } else {
        await createSourceWatch(request);
        setMessage("SourceWatch가 등록되었습니다.");
      }
      resetForm();
      await loadAll();
    } catch (submitError) {
      setError(getErrorMessage(submitError, "SourceWatch 저장에 실패했습니다."));
    } finally {
      setSaving(false);
    }
  }

  async function handleActiveToggle(sourceWatch: SourceWatch) {
    setMessage(null);
    setError(null);
    try {
      const updated = await updateSourceWatchActive(sourceWatch.id, { isActive: !sourceWatch.isActive });
      setSourceWatches((items) => items.map((item) => (item.id === updated.id ? updated : item)));
      setMessage("활성 상태가 변경되었습니다.");
    } catch (activeError) {
      setError(getErrorMessage(activeError, "활성 상태 변경에 실패했습니다."));
    }
  }

  async function handleCollect(sourceWatch: SourceWatch) {
    setCollectingId(sourceWatch.id);
    setMessage(null);
    setError(null);
    try {
      const result = await collectSourceWatch(sourceWatch.id);
      setCollectResults((current) => ({ ...current, [sourceWatch.id]: result }));
      setMessage(getCollectionOutcomeText(result));
      setSourceWatches(await getSourceWatches());
    } catch (collectError) {
      setError(getErrorMessage(collectError, "수집 실패"));
    } finally {
      setCollectingId(null);
    }
  }

  async function handleRegenerate(sourceWatch: SourceWatch) {
    setRegeneratingId(sourceWatch.id);
    setMessage(null);
    setError(null);
    try {
      const result = await regenerateSourceWatchCandidates(sourceWatch.id);
      setRegenerateResults((current) => ({ ...current, [sourceWatch.id]: result }));
      if (result.failureReason) {
        setMessage(result.message);
      } else {
        setMessage(`후보 재생성 완료: 신규 ${result.createdCandidateCount}개, 중복 ${result.skippedDuplicateCount}개`);
      }
      setSourceWatches(await getSourceWatches());
    } catch (regenerateError) {
      setError(`후보 재생성 실패: ${getErrorMessage(regenerateError, "최신 스냅샷이 없습니다.")}`);
    } finally {
      setRegeneratingId(null);
    }
  }

  async function handleTermsCheckSave(sourceWatch: SourceWatch) {
    const termsForm = termsForms[sourceWatch.id] ?? toTermsForm(sourceWatch);
    const request: SourceWatchTermsCheckRequest = {
      termsCheckStatus: termsForm.termsCheckStatus,
      termsUrl: normalizeOptional(termsForm.termsUrl),
      termsCheckedAt: normalizeOptional(termsForm.termsCheckedAt),
      termsMemo: normalizeOptional(termsForm.termsMemo),
    };

    setSavingTermsId(sourceWatch.id);
    setMessage(null);
    setError(null);
    try {
      const updated = await updateSourceWatchTermsCheck(sourceWatch.id, request);
      setSourceWatches((items) => items.map((item) => (item.id === updated.id ? updated : item)));
      setTermsForms((current) => ({ ...current, [updated.id]: toTermsForm(updated) }));
      setMessage("약관 확인 결과가 저장되었습니다.");
    } catch (termsError) {
      setError(getErrorMessage(termsError, "약관 확인 결과 저장에 실패했습니다."));
    } finally {
      setSavingTermsId(null);
    }
  }

  function updateTermsForm(sourceWatchId: number, patch: Partial<TermsFormState>) {
    setTermsForms((current) => ({
      ...current,
      [sourceWatchId]: {
        ...(current[sourceWatchId] ?? {
          termsCheckStatus: "NOT_CHECKED",
          termsUrl: "",
          termsCheckedAt: "",
          termsMemo: "",
        }),
        ...patch,
      },
    }));
  }

  async function handleToggleHistory(sourceWatchId: number) {
    if (expandedHistoryId === sourceWatchId) {
      setExpandedHistoryId(null);
      return;
    }

    setExpandedHistoryId(sourceWatchId);
    if (historyRuns[sourceWatchId] || historyLoadingId === sourceWatchId) {
      return;
    }

    setHistoryLoadingId(sourceWatchId);
    setHistoryErrors((current) => {
      const next = { ...current };
      delete next[sourceWatchId];
      return next;
    });
    try {
      const runs = await getSourceWatchCollectionRuns(sourceWatchId, 10);
      setHistoryRuns((current) => ({ ...current, [sourceWatchId]: runs }));
    } catch (historyError) {
      setHistoryErrors((current) => ({
        ...current,
        [sourceWatchId]: getErrorMessage(historyError, "최근 수집 이력을 불러오지 못했습니다."),
      }));
    } finally {
      setHistoryLoadingId(null);
    }
  }

  function isFixtureSourceWatch(sourceWatch: SourceWatch) {
    const title = (sourceWatch.title ?? "").toLowerCase();
    const url = (sourceWatch.url ?? "").toLowerCase();
    return (
      title.startsWith("local collection fixture") ||
      url.includes("/collection-fixtures/") ||
      url.includes("localhost:3000/collection-fixtures") ||
      url.includes("127.0.0.1:3000/collection-fixtures") ||
      url.includes("[::1]:3000/collection-fixtures")
    );
  }

  async function handleDeactivateFixtures() {
    const activeFixtures = sourceWatches.filter((sourceWatch) => isFixtureSourceWatch(sourceWatch) && sourceWatch.isActive);
    if (activeFixtures.length === 0) {
      setMessage("비활성화할 active fixture SourceWatch가 없습니다.");
      setError(null);
      return;
    }

    setDeactivatingFixtures(true);
    setMessage(null);
    setError(null);
    try {
      const results = await Promise.allSettled(
        activeFixtures.map((sourceWatch) => updateSourceWatchActive(sourceWatch.id, { isActive: false }))
      );
      await loadAll();
      if (results.some((result) => result.status === "rejected")) {
        setError("fixture 비활성화 중 일부 항목 처리에 실패했습니다.");
      } else {
        setMessage("fixture SourceWatch를 비활성화했습니다.");
      }
    } finally {
      setDeactivatingFixtures(false);
    }
  }

  const visibleSourceWatches = useMemo(
    () => (showFixtures ? sourceWatches : sourceWatches.filter((sourceWatch) => !isFixtureSourceWatch(sourceWatch))),
    [showFixtures, sourceWatches]
  );

  return (
    <section className="space-y-5">
      {message ? <Notice tone="success">{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}

      <div className="grid gap-5 xl:grid-cols-[420px_1fr]">
        <form className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm" onSubmit={handleSubmit}>
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-bold">{editing ? "공식 출처 수정" : "공식 출처 등록"}</h2>
            {editing ? (
              <button className="text-sm font-semibold text-blue-600" type="button" onClick={resetForm}>
                새로 등록
              </button>
            ) : null}
          </div>
          <FieldSelect label="브랜드" value={form.brandId} onChange={(brandId) => setForm((current) => ({ ...current, brandId }))} required>
            <option value="">선택</option>
            {brands.map((brand) => (
              <option key={brand.id} value={brand.id}>
                {brand.name}
              </option>
            ))}
          </FieldSelect>
          <div className="rounded-lg border border-neutral-200 bg-neutral-50 p-3 text-sm leading-6 text-neutral-700">
            <p>현재 등록된 브랜드는 {brands.length.toLocaleString()}개입니다.</p>
            <p className="mt-1">등록하려는 브랜드가 없다면 먼저 브랜드 관리에서 브랜드를 추가하세요.</p>
            <Link className="mt-2 inline-flex rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm font-semibold hover:border-blue-300" href="/admin/brands">
              브랜드 관리로 이동
            </Link>
          </div>
          <FieldSelect label="출처 유형" value={form.sourceType} onChange={(sourceType) => setForm((current) => ({ ...current, sourceType: sourceType as SourceType }))}>
            {SOURCE_TYPE_OPTIONS.map((sourceType) => (
              <option key={sourceType} value={sourceType}>
                {SOURCE_TYPE_LABELS[sourceType]}
              </option>
            ))}
          </FieldSelect>
          <TextInput label="수집 출처명" value={form.title} onChange={(title) => setForm((current) => ({ ...current, title }))} required />
          <TextInput label="수집 URL" value={form.url} onChange={(url) => setForm((current) => ({ ...current, url }))} required type="url" />
          <Checkbox label="활성 상태" checked={form.isActive} onChange={(isActive) => setForm((current) => ({ ...current, isActive }))} />
          <Checkbox label="로그인 필요 출처" checked={form.loginRequired} onChange={(loginRequired) => setForm((current) => ({ ...current, loginRequired }))} />
          <FieldSelect label="robots.txt 확인 상태" value={form.robotsCheckStatus} onChange={(robotsCheckStatus) => setForm((current) => ({ ...current, robotsCheckStatus: robotsCheckStatus as RobotsCheckStatus }))}>
            {(["UNKNOWN", "ALLOWED", "DISALLOWED", "NOT_FOUND", "FETCH_FAILED", "PARSE_FAILED"] as RobotsCheckStatus[]).map((status) => (
              <option key={status} value={status}>{status}</option>
            ))}
          </FieldSelect>
          <FieldSelect label="약관 확인 상태" value={form.termsCheckStatus} onChange={(termsCheckStatus) => setForm((current) => ({ ...current, termsCheckStatus: termsCheckStatus as TermsCheckStatus }))}>
            {(["NOT_CHECKED", "NO_RESTRICTION_FOUND", "NEEDS_REVIEW", "RESTRICTION_FOUND", "BLOCKED"] as TermsCheckStatus[]).map((status) => (
              <option key={status} value={status}>{termsCheckLabels[status]}</option>
            ))}
          </FieldSelect>
          <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
            약관 미확인 상태는 수집 실행을 막지 않습니다. 수집 실행 시 robots.txt와 접근 가능 여부를 먼저 자동 점검하고, 약관 수동 검토가 필요하면 NEEDS_REVIEW로 표시하세요.
          </p>
          <FieldSelect label="검증 방식" value={form.collectionMethod} onChange={(collectionMethod) => setForm((current) => ({ ...current, collectionMethod: collectionMethod as CollectionMethod }))}>
            {(["UNKNOWN", "AUTO_COLLECTED", "MANUAL_VERIFIED", "MIXED"] as CollectionMethod[]).map((method) => (
              <option key={method} value={method}>{method}</option>
            ))}
          </FieldSelect>
          <FieldSelect label="수동 권한 강제" value={form.collectionPermissionStatus} onChange={(collectionPermissionStatus) => setForm((current) => ({ ...current, collectionPermissionStatus: collectionPermissionStatus as CollectionPermissionStatus | "" }))}>
            <option value="">자동 계산</option>
            <option value="MANUAL_REVIEW_ONLY">MANUAL_REVIEW_ONLY</option>
          </FieldSelect>
          <TextArea label="정책 확인 메모" value={form.policyCheckNote} onChange={(policyCheckNote) => setForm((current) => ({ ...current, policyCheckNote }))} />
          <TextArea label="수동 검수 메모" value={form.manualVerificationNote} onChange={(manualVerificationNote) => setForm((current) => ({ ...current, manualVerificationNote }))} />
          <button className="h-11 w-full rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={saving}>
            {saving ? "저장 중" : editing ? "수정 저장" : "등록"}
          </button>
        </form>

        <div className="space-y-4">
          <div className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="text-sm leading-6 text-neutral-700">
                로컬 fixture 데이터는 자동 수집 E2E 검증용입니다. 실제 운영 SourceWatch 목록에서는 기본적으로 숨깁니다.
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <label className="flex items-center gap-2 text-sm font-medium">
                  <input checked={showFixtures} onChange={(event) => setShowFixtures(event.target.checked)} type="checkbox" />
                  테스트 fixture 포함 보기
                </label>
                <button
                  className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold disabled:opacity-60"
                  disabled={deactivatingFixtures}
                  onClick={handleDeactivateFixtures}
                  type="button"
                >
                  {deactivatingFixtures ? "처리 중" : "fixture 비활성화"}
                </button>
              </div>
            </div>
          </div>
          {loading ? <EmptyBox>SourceWatch 목록을 불러오는 중입니다.</EmptyBox> : null}
          {!loading && visibleSourceWatches.length === 0 ? <EmptyBox>표시할 SourceWatch가 없습니다.</EmptyBox> : null}
          {visibleSourceWatches.map((sourceWatch) => (
            <article
              key={sourceWatch.id}
              className={`rounded-2xl border bg-white p-5 shadow-sm ${
                highlightedSourceWatchId === sourceWatch.id ? "border-blue-400 ring-2 ring-blue-100" : "border-neutral-200"
              }`}
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-bold">{sourceWatch.title ?? "-"}</h3>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${COLLECTION_STATUS_CLASS[sourceWatch.lastStatus] ?? "bg-neutral-100 text-neutral-700"}`}>
                      {COLLECTION_STATUS_LABELS[sourceWatch.lastStatus] ?? sourceWatch.lastStatus}
                    </span>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${sourceWatch.isActive ? "bg-green-50 text-green-700" : "bg-neutral-100 text-neutral-700"}`}>
                      {sourceWatch.isActive ? "활성" : "비활성"}
                    </span>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${permissionClasses[sourceWatch.collectionPermissionStatus]}`}>
                      {permissionLabels[sourceWatch.collectionPermissionStatus]}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-neutral-500">
                    {sourceWatch.brandName ?? "-"} · {SOURCE_TYPE_LABELS[sourceWatch.sourceType] ?? sourceWatch.sourceType ?? "-"}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={() => startEdit(sourceWatch)}>
                    수정
                  </button>
                  <button className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={() => handleActiveToggle(sourceWatch)}>
                    {sourceWatch.isActive ? "비활성" : "활성"}
                  </button>
                  <button
                    className="h-9 rounded-lg bg-blue-600 px-3 text-sm font-semibold text-white disabled:opacity-60"
                    type="button"
                    disabled={!canCollect(sourceWatch) || collectingId === sourceWatch.id}
                    onClick={() => handleCollect(sourceWatch)}
                  >
                    {collectingId === sourceWatch.id ? "수집 중..." : getCollectButtonLabel(sourceWatch)}
                  </button>
                  {!isRegenerationBlocked(sourceWatch) ? (
                    <button
                      className="h-9 rounded-lg border border-blue-600 px-3 text-sm font-semibold text-blue-600 disabled:opacity-60"
                      type="button"
                      disabled={regeneratingId === sourceWatch.id}
                      onClick={() => handleRegenerate(sourceWatch)}
                      title="기존 최신 스냅샷을 개선된 추출 규칙으로 다시 분석합니다. 외부 URL을 다시 호출하지 않습니다."
                    >
                      {regeneratingId === sourceWatch.id ? "재생성 중" : "후보 재생성"}
                    </button>
                  ) : null}
                  <Link className="h-9 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-semibold" href="/admin/collection-runs">
                    최근 수집 이력 보기
                  </Link>
                  <a className="h-9 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-semibold" href={sourceWatch.url} target="_blank" rel="noreferrer">
                    공식 페이지 열기
                  </a>
                  <Link className="h-9 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-semibold" href={`/admin/benefits?keyword=${encodeURIComponent(sourceWatch.brandName)}`}>
                    공개 혜택 관리
                  </Link>
                  <Link className="h-9 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white" href={`/admin/benefits/new?brandId=${sourceWatch.brandId}&sourceWatchId=${sourceWatch.id}`}>
                    수동 혜택 등록
                  </Link>
                  <button
                    className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold"
                    type="button"
                    onClick={() => handleToggleHistory(sourceWatch.id)}
                  >
                    {expandedHistoryId === sourceWatch.id ? "이력 접기" : "최근 수집 이력"}
                  </button>
                  <Link className="h-9 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-semibold" href={`/admin/collection-runs?sourceWatchId=${sourceWatch.id}`}>
                    전체 이력 보기
                  </Link>
                </div>
              </div>
              <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
                <Info label="수집 URL" value={sourceWatch.url ?? "-"} wide />
                <Info label="최근 수집 일시" value={formatDateTime(sourceWatch.lastFetchedAt)} />
                <Info label="실패 횟수" value={String(sourceWatch.failureCount ?? 0)} />
                <Info label="최근 content hash" value={sourceWatch.lastContentHash ? `${sourceWatch.lastContentHash.slice(0, 16)}...` : "-"} />
                <Info label="다음 수집 예정" value={formatDateTime(sourceWatch.nextFetchAt)} />
                <Info label="정책 상태" value={permissionLabels[sourceWatch.collectionPermissionStatus]} />
                <Info label="robots.txt" value={sourceWatch.robotsCheckStatus} />
                <Info label="약관 확인" value={sourceWatch.termsCheckStatus} />
                <Info label="로그인 필요" value={sourceWatch.loginRequired ? "예" : "아니오"} />
                <Info label="정책 확인일" value={formatDateTime(sourceWatch.lastPolicyCheckedAt)} />
                <Info label="수동 검수일" value={formatDateTime(sourceWatch.lastManualVerifiedAt)} />
                <Info label="정책 메모" value={sourceWatch.policyCheckNote ?? "-"} wide />
                <Info label="수동 검수 메모" value={sourceWatch.manualVerificationNote ?? "-"} wide />
              </dl>
              <p className="mt-4 rounded-lg bg-neutral-50 p-3 text-sm leading-6 text-neutral-700">
                {getPolicyGuidance(sourceWatch)}
              </p>
              {isRegenerationBlocked(sourceWatch) ? (
                <p className="mt-3 rounded-lg bg-amber-50 p-3 text-sm leading-6 text-amber-800">
                  후보 재생성은 정책 점검 결과 수집 가능한 출처의 저장된 최신 스냅샷에만 사용할 수 있습니다.
                  미확인 출처는 먼저 수집 실행으로 자동 점검을 수행해 주세요.
                </p>
              ) : null}
              <section className="mt-4 rounded-xl border border-neutral-200 bg-white p-4">
                <div>
                  <h4 className="text-sm font-bold text-neutral-950">약관 확인</h4>
                  <p className="mt-1 text-xs leading-5 text-neutral-500">
                    자동 탐색된 약관 후보 링크입니다. 직접 열어 확인한 뒤 상태를 선택하세요.
                  </p>
                </div>
                {(sourceWatch.termsLinkCandidates ?? []).length > 0 ? (
                  <div className="mt-3 space-y-2">
                    {sourceWatch.termsLinkCandidates.map((candidate) => (
                      <a
                        key={`${candidate.type}-${candidate.url}`}
                        className="flex flex-col gap-1 rounded-lg border border-neutral-200 bg-neutral-50 p-3 text-xs transition hover:border-blue-200 hover:bg-blue-50 sm:flex-row sm:items-center sm:justify-between"
                        href={candidate.url}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <span className="flex flex-wrap items-center gap-2">
                          <span className={`rounded-full px-2 py-1 font-semibold ${termsCandidateClass[candidate.type] ?? termsCandidateClass.OTHER}`}>
                            {termsCandidateTypeLabels[candidate.type] ?? candidate.type}
                          </span>
                          <span className="font-semibold text-neutral-800">{candidate.label}</span>
                          <span className="text-neutral-400">{candidate.confidence}</span>
                        </span>
                        <span className="break-all text-neutral-500">{candidate.url}</span>
                      </a>
                    ))}
                  </div>
                ) : (
                  <p className="mt-3 rounded-lg bg-neutral-50 p-3 text-xs leading-5 text-neutral-500">
                    수집 대상 페이지를 읽지 못했거나 약관 후보 링크를 자동 탐색하지 못했습니다.
                  </p>
                )}
                <div className="mt-4 grid gap-3 md:grid-cols-2">
                  <label className="text-xs font-semibold text-neutral-700">
                    약관 상태
                    <select
                      className="mt-1 h-10 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm font-normal"
                      value={(termsForms[sourceWatch.id] ?? toTermsForm(sourceWatch)).termsCheckStatus}
                      onChange={(event) => updateTermsForm(sourceWatch.id, { termsCheckStatus: event.target.value as TermsCheckStatus })}
                    >
                      {(["NOT_CHECKED", "NO_RESTRICTION_FOUND", "NEEDS_REVIEW", "RESTRICTION_FOUND", "BLOCKED"] as TermsCheckStatus[]).map((status) => (
                        <option key={status} value={status}>
                          {termsCheckLabels[status]}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="text-xs font-semibold text-neutral-700">
                    약관 확인일
                    <input
                      className="mt-1 h-10 w-full rounded-lg border border-neutral-200 px-3 text-sm font-normal"
                      type="date"
                      value={(termsForms[sourceWatch.id] ?? toTermsForm(sourceWatch)).termsCheckedAt}
                      onChange={(event) => updateTermsForm(sourceWatch.id, { termsCheckedAt: event.target.value })}
                    />
                  </label>
                  <label className="text-xs font-semibold text-neutral-700 md:col-span-2">
                    약관 URL
                    <input
                      className="mt-1 h-10 w-full rounded-lg border border-neutral-200 px-3 text-sm font-normal"
                      type="url"
                      value={(termsForms[sourceWatch.id] ?? toTermsForm(sourceWatch)).termsUrl}
                      onChange={(event) => updateTermsForm(sourceWatch.id, { termsUrl: event.target.value })}
                      placeholder="https://example.com/terms"
                    />
                  </label>
                  <label className="text-xs font-semibold text-neutral-700 md:col-span-2">
                    약관 확인 메모
                    <textarea
                      className="mt-1 min-h-20 w-full rounded-lg border border-neutral-200 px-3 py-2 text-sm font-normal"
                      value={(termsForms[sourceWatch.id] ?? toTermsForm(sourceWatch)).termsMemo}
                      onChange={(event) => updateTermsForm(sourceWatch.id, { termsMemo: event.target.value })}
                      placeholder="자동 수집 금지 문구, 이미지/상표 사용 제한, 수동 검토 필요 사항을 기록하세요."
                    />
                  </label>
                </div>
                <button
                  className="mt-3 h-9 rounded-lg bg-blue-600 px-3 text-sm font-semibold text-white disabled:opacity-60"
                  type="button"
                  disabled={savingTermsId === sourceWatch.id}
                  onClick={() => handleTermsCheckSave(sourceWatch)}
                >
                  {savingTermsId === sourceWatch.id ? "저장 중" : "약관 확인 결과 저장"}
                </button>
              </section>
              <div className="mt-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-semibold text-neutral-800">최근 CollectionRun</span>
                  {sourceWatch.recentCollectionRun ? (
                    <span
                      className={`rounded-full px-3 py-1 text-xs font-semibold ${
                        COLLECTION_STATUS_CLASS[sourceWatch.recentCollectionRun.status] ?? "bg-neutral-100 text-neutral-700"
                      }`}
                    >
                      {COLLECTION_STATUS_LABELS[sourceWatch.recentCollectionRun.status] ?? sourceWatch.recentCollectionRun.status}
                    </span>
                  ) : null}
                </div>
                {sourceWatch.recentCollectionRun ? (
                  <dl className="mt-3 grid gap-3 text-sm md:grid-cols-2">
                    <Info label="최근 수집 상태" value={COLLECTION_STATUS_LABELS[sourceWatch.recentCollectionRun.status] ?? sourceWatch.recentCollectionRun.status} />
                    <Info label="최근 수집 시각" value={formatDateTime(sourceWatch.recentCollectionRun.startedAt)} />
                    <Info label="최근 후보 생성 수" value={`${sourceWatch.recentCollectionRun.candidateCount ?? 0}개`} />
                    <Info label="robots.txt 확인 결과" value={getRobotsSummary(sourceWatch.recentCollectionRun).label} />
                    <Info
                      label="최근 실패/스킵 사유"
                      value={getFailureReasonLabel(sourceWatch.recentCollectionRun.failureReason) ?? "-"}
                    />
                    <Info
                      label="상세 원인"
                      value={
                        getRobotsSummary(sourceWatch.recentCollectionRun).detail ??
                        sourceWatch.recentCollectionRun.errorMessage ??
                        "-"
                      }
                    />
                  </dl>
                ) : (
                  <p className="mt-3 text-sm text-neutral-500">아직 수집 이력이 없습니다.</p>
                )}
              </div>
              <div className="mt-4 grid gap-2 text-xs text-neutral-500 md:grid-cols-2">
                <p className="rounded-lg bg-neutral-50 p-3">
                  <span className="font-semibold text-neutral-700">수집 실행</span>: 도메인 최소 간격과 robots.txt를 확인한 뒤 허용된 공식 URL만 다시 가져옵니다.
                </p>
                <p className="rounded-lg bg-neutral-50 p-3">
                  <span className="font-semibold text-neutral-700">후보 재생성</span>: 정책상 허용된 출처의 저장된 최신 스냅샷만 다시 분석합니다.
                </p>
              </div>
              {collectResults[sourceWatch.id] ? (
                <div className={`mt-4 rounded-lg p-3 text-sm ${collectResults[sourceWatch.id].failureReason ? "bg-amber-50 text-amber-800" : "bg-blue-50 text-blue-800"}`}>
                  {collectResults[sourceWatch.id].failureReason ? (
                    <>
                      {skippedFailureReasons.has(collectResults[sourceWatch.id].failureReason ?? "") ? "수집 건너뜀" : "수집 실패"} · 사유:{" "}
                      {getFailureReasonLabel(collectResults[sourceWatch.id].failureReason) ?? collectResults[sourceWatch.id].failureReason}
                      <p className="mt-1 text-xs leading-5">{collectResults[sourceWatch.id].message}</p>
                    </>
                  ) : (
                    <>
                      수집 완료 · 후보: {collectResults[sourceWatch.id].candidateCount} · sameAsPrevious:{" "}
                      {String(collectResults[sourceWatch.id].sameAsPrevious)}
                    </>
                  )}
                </div>
              ) : null}
              {expandedHistoryId === sourceWatch.id ? (
                <div className="mt-4 rounded-xl border border-neutral-200 bg-white p-4">
                  <div className="flex items-center justify-between gap-3">
                    <h4 className="text-sm font-semibold text-neutral-900">최근 수집 이력</h4>
                    <span className="text-xs text-neutral-500">최근 10건</span>
                  </div>
                  {historyLoadingId === sourceWatch.id ? (
                    <p className="mt-3 text-sm text-neutral-500">최근 수집 이력을 불러오는 중입니다.</p>
                  ) : null}
                  {historyErrors[sourceWatch.id] ? (
                    <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">{historyErrors[sourceWatch.id]}</p>
                  ) : null}
                  {!historyLoadingId && !historyErrors[sourceWatch.id] && (historyRuns[sourceWatch.id]?.length ?? 0) === 0 ? (
                    <p className="mt-3 text-sm text-neutral-500">표시할 수집 이력이 없습니다.</p>
                  ) : null}
                  {!historyLoadingId && !historyErrors[sourceWatch.id] && historyRuns[sourceWatch.id]?.length ? (
                    <div className="mt-3 space-y-3">
                      {historyRuns[sourceWatch.id].map((run) => (
                        <article key={run.id} className="rounded-lg border border-neutral-200 bg-neutral-50 p-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${COLLECTION_STATUS_CLASS[run.status] ?? "bg-neutral-100 text-neutral-700"}`}>
                              {COLLECTION_STATUS_LABELS[run.status] ?? run.status}
                            </span>
                            <span className="text-sm font-semibold text-neutral-800">
                              {run.failureReason ? getFailureReasonLabel(run.failureReason) : run.message}
                            </span>
                          </div>
                          <div className="mt-2 grid gap-2 text-xs text-neutral-500 md:grid-cols-3">
                            <p>{formatDateTime(run.startedAt)}</p>
                            <p>후보 {run.candidateCount ?? 0}개</p>
                            <p>{run.snapshotId ? `snapshotId=${run.snapshotId}` : "snapshot 없음"}</p>
                          </div>
                          {run.failureReason || run.detailReason ? (
                            <p className="mt-2 text-xs leading-5 text-neutral-600">
                              {run.detailReason ?? run.message}
                            </p>
                          ) : null}
                        </article>
                      ))}
                    </div>
                  ) : null}
                </div>
              ) : null}
              {regenerateResults[sourceWatch.id] ? (
                <div
                  className={`mt-4 rounded-lg p-3 text-sm ${
                    regenerateResults[sourceWatch.id].failureReason
                      ? "bg-amber-50 text-amber-800"
                      : "bg-green-50 text-green-800"
                  }`}
                >
                  {regenerateResults[sourceWatch.id].failureReason ? "후보 재생성 건너뜀" : "후보 재생성 완료"} · 신규:{" "}
                  {regenerateResults[sourceWatch.id].createdCandidateCount} · 중복:{" "}
                  {regenerateResults[sourceWatch.id].skippedDuplicateCount} · snapshotId:{" "}
                  {regenerateResults[sourceWatch.id].snapshotId ?? "-"} · runId:{" "}
                  {regenerateResults[sourceWatch.id].collectionRunId}
                  <Link
                    className="ml-3 font-semibold underline"
                    href={`/admin/benefit-candidates?collectionRunId=${regenerateResults[sourceWatch.id].collectionRunId}`}
                  >
                    후보 목록 보기
                  </Link>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function FieldSelect({
  children,
  label,
  onChange,
  required,
  value,
}: {
  children: React.ReactNode;
  label: string;
  onChange: (value: string) => void;
  required?: boolean;
  value: string;
}) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <select className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required={required}>
        {children}
      </select>
    </label>
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

function TextArea({ label, onChange, value }: { label: string; onChange: (value: string) => void; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <textarea className="min-h-24 w-full rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm leading-6" value={value} onChange={(event) => onChange(event.target.value)} />
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

function Info({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={wide ? "md:col-span-2" : undefined}>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 break-all text-neutral-800">{value}</dd>
    </div>
  );
}

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-600 shadow-sm">{children}</div>;
}

function Notice({ children, tone }: { children: React.ReactNode; tone: "success" | "error" }) {
  const className = tone === "success" ? "border-green-200 bg-green-50 text-green-700" : "border-red-200 bg-red-50 text-red-700";
  return <div className={`rounded-2xl border p-4 text-sm ${className}`}>{children}</div>;
}
