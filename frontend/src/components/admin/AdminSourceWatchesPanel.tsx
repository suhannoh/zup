"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import {
  collectSourceWatch,
  createSourceWatch,
  getAdminBrands,
  getSourceWatches,
  regenerateSourceWatchCandidates,
  updateSourceWatch,
  updateSourceWatchActive,
} from "@/lib/api/adminApi";
import { COLLECTION_STATUS_CLASS, COLLECTION_STATUS_LABELS } from "@/lib/adminLabels";
import { SOURCE_TYPE_LABELS, SOURCE_TYPE_OPTIONS } from "@/lib/sourceLabels";
import type { SourceType } from "@/types/adminBenefitSource";
import type { AdminBrand } from "@/types/adminBrand";
import type {
  SourceWatch,
  SourceWatchCollectResponse,
  SourceWatchRegenerateCandidatesResponse,
} from "@/types/sourceWatch";

type FormState = {
  brandId: string;
  sourceType: SourceType;
  title: string;
  url: string;
  isActive: boolean;
};

const emptyForm: FormState = {
  brandId: "",
  sourceType: "OFFICIAL_HOME",
  title: "",
  url: "",
  isActive: true,
};

function toForm(sourceWatch: SourceWatch): FormState {
  return {
    brandId: String(sourceWatch.brandId),
    sourceType: sourceWatch.sourceType,
    title: sourceWatch.title,
    url: sourceWatch.url,
    isActive: sourceWatch.isActive,
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

export function AdminSourceWatchesPanel() {
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

  async function loadAll() {
    setLoading(true);
    setError(null);
    try {
      const [watchData, brandData] = await Promise.all([getSourceWatches(), getAdminBrands()]);
      setSourceWatches(watchData);
      setBrands(brandData);
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
      setMessage(`수집 완료: 후보 ${result.candidateCount}개, 동일 HTML 여부 ${String(result.sameAsPrevious)}`);
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
      setMessage(`후보 재생성 완료: 신규 ${result.createdCandidateCount}개, 중복 ${result.skippedDuplicateCount}개`);
    } catch (regenerateError) {
      setError(`후보 재생성 실패: ${getErrorMessage(regenerateError, "최신 스냅샷이 없습니다.")}`);
    } finally {
      setRegeneratingId(null);
    }
  }

  function isFixtureSourceWatch(sourceWatch: SourceWatch) {
    const title = sourceWatch.title.toLowerCase();
    const url = sourceWatch.url.toLowerCase();
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

  const visibleSourceWatches = showFixtures
    ? sourceWatches
    : sourceWatches.filter((sourceWatch) => !isFixtureSourceWatch(sourceWatch));

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
            <article key={sourceWatch.id} className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-bold">{sourceWatch.title}</h3>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${COLLECTION_STATUS_CLASS[sourceWatch.lastStatus] ?? "bg-neutral-100 text-neutral-700"}`}>
                      {COLLECTION_STATUS_LABELS[sourceWatch.lastStatus] ?? sourceWatch.lastStatus}
                    </span>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${sourceWatch.isActive ? "bg-green-50 text-green-700" : "bg-neutral-100 text-neutral-700"}`}>
                      {sourceWatch.isActive ? "활성" : "비활성"}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-neutral-500">
                    {sourceWatch.brandName} · {SOURCE_TYPE_LABELS[sourceWatch.sourceType]}
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
                    disabled={!sourceWatch.isActive || collectingId === sourceWatch.id}
                    onClick={() => handleCollect(sourceWatch)}
                  >
                    {collectingId === sourceWatch.id ? "수집 중" : "수집 실행"}
                  </button>
                  <button
                    className="h-9 rounded-lg border border-blue-600 px-3 text-sm font-semibold text-blue-600 disabled:opacity-60"
                    type="button"
                    disabled={regeneratingId === sourceWatch.id}
                    onClick={() => handleRegenerate(sourceWatch)}
                    title="기존 최신 스냅샷을 개선된 추출 규칙으로 다시 분석합니다. 외부 URL을 다시 호출하지 않습니다."
                  >
                    {regeneratingId === sourceWatch.id ? "재생성 중" : "후보 재생성"}
                  </button>
                  <Link className="h-9 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-semibold" href="/admin/collection-runs">
                    최근 수집 이력 보기
                  </Link>
                </div>
              </div>
              <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
                <Info label="수집 URL" value={sourceWatch.url} wide />
                <Info label="최근 수집 일시" value={formatDateTime(sourceWatch.lastFetchedAt)} />
                <Info label="실패 횟수" value={String(sourceWatch.failureCount)} />
                <Info label="최근 content hash" value={sourceWatch.lastContentHash ? `${sourceWatch.lastContentHash.slice(0, 16)}...` : "-"} />
                <Info label="다음 수집 예정" value={formatDateTime(sourceWatch.nextFetchAt)} />
              </dl>
              <div className="mt-4 grid gap-2 text-xs text-neutral-500 md:grid-cols-2">
                <p className="rounded-lg bg-neutral-50 p-3">
                  <span className="font-semibold text-neutral-700">수집 실행</span>: 공식 URL을 다시 가져옵니다.
                </p>
                <p className="rounded-lg bg-neutral-50 p-3">
                  <span className="font-semibold text-neutral-700">후보 재생성</span>: 저장된 최신 스냅샷을 다시 분석합니다.
                </p>
              </div>
              {collectResults[sourceWatch.id] ? (
                <div className="mt-4 rounded-lg bg-blue-50 p-3 text-sm text-blue-800">
                  수집 완료 · 후보: {collectResults[sourceWatch.id].candidateCount} · sameAsPrevious:{" "}
                  {String(collectResults[sourceWatch.id].sameAsPrevious)}
                </div>
              ) : null}
              {regenerateResults[sourceWatch.id] ? (
                <div className="mt-4 rounded-lg bg-green-50 p-3 text-sm text-green-800">
                  후보 재생성 완료 · 신규: {regenerateResults[sourceWatch.id].createdCandidateCount} · 중복:{" "}
                  {regenerateResults[sourceWatch.id].skippedDuplicateCount} · snapshotId:{" "}
                  {regenerateResults[sourceWatch.id].snapshotId}
                  <Link className="ml-3 font-semibold underline" href="/admin/benefit-candidates">
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
