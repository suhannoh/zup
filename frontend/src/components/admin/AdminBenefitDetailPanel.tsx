"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  BIRTHDAY_TIMING_LABELS,
  BENEFIT_TYPE_LABELS,
  VERIFICATION_STATUS_LABELS,
} from "@/lib/benefitLabels";
import {
  addAdminBenefitTag,
  createAdminBenefitSource,
  deleteAdminBenefitSource,
  deleteAdminBenefitTag,
  getAdminBenefit,
  getAdminBenefitSources,
  getAdminBenefitVerificationLogs,
  updateAdminBenefitSource,
} from "@/lib/api/adminApi";
import { getTags } from "@/lib/api/publicApi";
import { SOURCE_TYPE_LABELS, SOURCE_TYPE_OPTIONS } from "@/lib/sourceLabels";
import type { AdminBenefit } from "@/types/adminBenefit";
import type {
  AdminBenefitSource,
  AdminBenefitSourceCreateRequest,
  SourceType,
} from "@/types/adminBenefitSource";
import type { Tag } from "@/types/tag";
import type { VerificationLog } from "@/types/verificationLog";

type SourceFormState = {
  sourceType: SourceType;
  sourceUrl: string;
  sourceTitle: string;
  sourceCheckedAt: string;
  memo: string;
};

const emptySourceForm: SourceFormState = {
  sourceType: "OFFICIAL_HOME",
  sourceUrl: "",
  sourceTitle: "",
  sourceCheckedAt: "",
  memo: "",
};

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toSourceRequest(form: SourceFormState): AdminBenefitSourceCreateRequest {
  return {
    sourceType: form.sourceType,
    sourceUrl: form.sourceUrl.trim(),
    sourceTitle: normalizeOptional(form.sourceTitle),
    sourceCheckedAt: normalizeOptional(form.sourceCheckedAt),
    memo: normalizeOptional(form.memo),
  };
}

function toSourceForm(source: AdminBenefitSource): SourceFormState {
  return {
    sourceType: source.sourceType,
    sourceUrl: source.sourceUrl,
    sourceTitle: source.sourceTitle ?? "",
    sourceCheckedAt: source.sourceCheckedAt ?? "",
    memo: source.memo ?? "",
  };
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function AdminBenefitDetailPanel({ benefitId }: { benefitId: number }) {
  const [benefit, setBenefit] = useState<AdminBenefit | null>(null);
  const [sources, setSources] = useState<AdminBenefitSource[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [logs, setLogs] = useState<VerificationLog[]>([]);
  const [sourceForm, setSourceForm] = useState<SourceFormState>(emptySourceForm);
  const [editingSource, setEditingSource] = useState<AdminBenefitSource | null>(null);
  const [selectedTagId, setSelectedTagId] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingSource, setSavingSource] = useState(false);
  const [savingTag, setSavingTag] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const activeSources = sources.filter((source) => source.isActive);

  const connectedTagSlugs = useMemo(
    () => new Set((benefit?.tags ?? []).map((tag) => tag.slug)),
    [benefit]
  );

  const selectedTagAlreadyConnected = useMemo(() => {
    const selected = tags.find((tag) => String(tag.id) === selectedTagId);
    return selected ? connectedTagSlugs.has(selected.slug) : false;
  }, [connectedTagSlugs, selectedTagId, tags]);

  async function loadAll() {
    setLoading(true);
    setError(null);
    try {
      const [benefitData, sourceData, tagData, logData] = await Promise.all([
        getAdminBenefit(benefitId),
        getAdminBenefitSources(benefitId),
        getTags(),
        getAdminBenefitVerificationLogs(benefitId),
      ]);
      setBenefit(benefitData);
      setSources(sourceData);
      setTags(tagData);
      setLogs(logData);
    } catch {
      setError("혜택 운영 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function reloadBenefitAndLogs() {
    const [benefitData, logData] = await Promise.all([
      getAdminBenefit(benefitId),
      getAdminBenefitVerificationLogs(benefitId),
    ]);
    setBenefit(benefitData);
    setLogs(logData);
  }

  useEffect(() => {
    loadAll();
  }, [benefitId]);

  function updateSourceForm(next: Partial<SourceFormState>) {
    setSourceForm((current) => ({ ...current, ...next }));
  }

  function startEditSource(source: AdminBenefitSource) {
    setEditingSource(source);
    setSourceForm(toSourceForm(source));
    setMessage(null);
    setError(null);
  }

  function resetSourceForm() {
    setEditingSource(null);
    setSourceForm(emptySourceForm);
  }

  async function handleSourceSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sourceForm.sourceUrl.trim()) {
      setError("출처 URL은 필수입니다.");
      return;
    }

    setSavingSource(true);
    setMessage(null);
    setError(null);

    try {
      if (editingSource) {
        await updateAdminBenefitSource(editingSource.id, toSourceRequest(sourceForm));
        setMessage("출처 정보가 수정되었습니다.");
      } else {
        await createAdminBenefitSource(benefitId, toSourceRequest(sourceForm));
        setMessage("공식 출처가 등록되었습니다.");
      }
      resetSourceForm();
      setSources(await getAdminBenefitSources(benefitId));
      await reloadBenefitAndLogs();
    } catch {
      setError("출처 저장에 실패했습니다.");
    } finally {
      setSavingSource(false);
    }
  }

  async function handleDeleteSource(sourceId: number) {
    setMessage(null);
    setError(null);

    try {
      await deleteAdminBenefitSource(sourceId);
      setSources(await getAdminBenefitSources(benefitId));
      await reloadBenefitAndLogs();
      setMessage("출처가 비활성화되었습니다.");
    } catch {
      setError("출처 저장에 실패했습니다.");
    }
  }

  async function handleAddTag(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedTagId) {
      setError("추가할 태그를 선택해 주세요.");
      return;
    }
    if (selectedTagAlreadyConnected) {
      setError("이미 연결된 태그입니다.");
      return;
    }

    setSavingTag(true);
    setMessage(null);
    setError(null);

    try {
      const updated = await addAdminBenefitTag(benefitId, { tagId: Number(selectedTagId) });
      setBenefit(updated);
      setSelectedTagId("");
      setMessage("태그가 연결되었습니다.");
    } catch {
      setError("태그 처리에 실패했습니다.");
    } finally {
      setSavingTag(false);
    }
  }

  async function handleDeleteTag(tagSlug: string) {
    const tag = tags.find((item) => item.slug === tagSlug);
    if (!tag) {
      setError("태그 처리에 실패했습니다.");
      return;
    }

    setSavingTag(true);
    setMessage(null);
    setError(null);

    try {
      const updated = await deleteAdminBenefitTag(benefitId, tag.id);
      setBenefit(updated);
      setMessage("태그 연결이 해제되었습니다.");
    } catch {
      setError("태그 처리에 실패했습니다.");
    } finally {
      setSavingTag(false);
    }
  }

  if (loading) {
    return <EmptyBox>혜택 운영 정보를 불러오는 중입니다.</EmptyBox>;
  }

  if (!benefit) {
    return (
      <div className="space-y-4">
        <BackLink />
        <Notice tone="error">{error ?? "혜택 운영 정보를 불러오지 못했습니다."}</Notice>
      </div>
    );
  }

  return (
    <section className="space-y-5">
      <BackLink />
      {message ? <Notice tone="success">{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}

      <section className="rounded-lg border border-border bg-white p-5">
        <h2 className="text-lg font-semibold">혜택 요약</h2>
        <div className="mt-4 grid gap-3 text-sm md:grid-cols-2">
          <Info label="혜택명" value={benefit.title} />
          <Info label="브랜드명" value={benefit.brandName} />
          <Info label="요약" value={benefit.summary} wide />
          <Info label="혜택 유형" value={BENEFIT_TYPE_LABELS[benefit.benefitType]} />
          <Info label="생일 기간 유형" value={BIRTHDAY_TIMING_LABELS[benefit.birthdayTimingType]} />
          <Info label="검수 상태" value={VERIFICATION_STATUS_LABELS[benefit.verificationStatus]} />
          <Info label="최근 확인일" value={benefit.lastVerifiedAt ?? "-"} />
          <Info label="활성 상태" value={benefit.isActive ? "활성" : "비활성"} />
        </div>
        <p className="mt-4 rounded-lg bg-neutral-50 p-3 text-xs leading-5 text-neutral-600">
          PUBLISHED 상태이면서 활성화된 혜택만 사용자 화면에 노출됩니다.
        </p>
      </section>

      <section className="grid gap-5 xl:grid-cols-[360px_1fr]">
        <form className="space-y-4 rounded-lg border border-border bg-white p-5" onSubmit={handleSourceSubmit}>
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">{editingSource ? "출처 수정" : "공식 출처 등록"}</h2>
            {editingSource ? (
              <button className="text-sm font-semibold text-accent" type="button" onClick={resetSourceForm}>
                새로 등록
              </button>
            ) : null}
          </div>
          <p className="rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">
            블로그/커뮤니티 출처는 수집 힌트로만 사용하고, 게시 근거는 공식 출처를 우선합니다.
          </p>
          <Select label="출처 유형" value={sourceForm.sourceType} onChange={(value) => updateSourceForm({ sourceType: value as SourceType })}>
            {SOURCE_TYPE_OPTIONS.map((type) => (
              <option key={type} value={type}>
                {SOURCE_TYPE_LABELS[type]}
              </option>
            ))}
          </Select>
          <TextInput label="출처 URL" value={sourceForm.sourceUrl} onChange={(value) => updateSourceForm({ sourceUrl: value })} required type="url" />
          <TextInput label="출처 제목" value={sourceForm.sourceTitle} onChange={(value) => updateSourceForm({ sourceTitle: value })} />
          <TextInput label="확인일" value={sourceForm.sourceCheckedAt} onChange={(value) => updateSourceForm({ sourceCheckedAt: value })} type="date" />
          <TextArea label="메모" value={sourceForm.memo} onChange={(value) => updateSourceForm({ memo: value })} />
          <button className="h-11 w-full rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={savingSource}>
            {savingSource ? "저장 중" : editingSource ? "출처 수정" : "출처 등록"}
          </button>
        </form>

        <div className="rounded-lg border border-border bg-white p-5">
          <h2 className="text-lg font-semibold">공식 출처 관리</h2>
          <div className="mt-4 space-y-3">
            {activeSources.length === 0 ? <EmptyBox>등록된 공식 출처가 없습니다.</EmptyBox> : null}
            {activeSources.map((source) => (
              <article key={source.id} className="rounded-lg border border-border p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                      {SOURCE_TYPE_LABELS[source.sourceType]}
                    </span>
                    <h3 className="mt-3 font-semibold">{source.sourceTitle ?? "제목 없음"}</h3>
                  </div>
                  <div className="flex gap-2">
                    <button className="h-9 rounded-lg border border-border px-3 text-sm font-semibold" type="button" onClick={() => startEditSource(source)}>
                      수정
                    </button>
                    <button className="h-9 rounded-lg border border-border px-3 text-sm font-semibold" type="button" onClick={() => handleDeleteSource(source.id)}>
                      삭제
                    </button>
                  </div>
                </div>
                <dl className="mt-3 grid gap-3 text-sm md:grid-cols-2">
                  <div className="md:col-span-2">
                    <dt className="font-medium text-neutral-500">출처 URL</dt>
                    <dd className="mt-1 break-all">
                      <a className="text-accent hover:underline" href={source.sourceUrl} target="_blank" rel="noreferrer">
                        {source.sourceUrl}
                      </a>
                    </dd>
                  </div>
                  <Info label="확인일" value={source.sourceCheckedAt ?? "-"} />
                  <Info label="활성 상태" value={source.isActive ? "활성" : "비활성"} />
                  <Info label="메모" value={source.memo ?? "-"} wide />
                </dl>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="rounded-lg border border-border bg-white p-5">
        <h2 className="text-lg font-semibold">태그 관리</h2>
        <div className="mt-4 flex flex-wrap gap-2">
          {(benefit.tags ?? []).length === 0 ? <span className="text-sm text-neutral-500">연결된 태그가 없습니다.</span> : null}
          {(benefit.tags ?? []).map((tag) => (
            <span key={tag.slug} className="inline-flex items-center gap-2 rounded-full bg-neutral-100 px-3 py-1 text-sm font-medium">
              {tag.name}
              <button className="text-neutral-500" type="button" disabled={savingTag} onClick={() => handleDeleteTag(tag.slug)}>
                삭제
              </button>
            </span>
          ))}
        </div>
        <form className="mt-4 flex flex-col gap-3 sm:flex-row" onSubmit={handleAddTag}>
          <select className="h-11 flex-1 rounded-lg border border-border bg-white px-3 text-sm" value={selectedTagId} onChange={(event) => setSelectedTagId(event.target.value)}>
            <option value="">태그 선택</option>
            {tags.map((tag) => (
              <option key={tag.id} value={tag.id} disabled={connectedTagSlugs.has(tag.slug)}>
                {tag.name}
              </option>
            ))}
          </select>
          <button className="h-11 rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={savingTag || selectedTagAlreadyConnected}>
            태그 추가
          </button>
        </form>
      </section>

      <section className="rounded-lg border border-border bg-white p-5">
        <h2 className="text-lg font-semibold">검수 이력</h2>
        <div className="mt-4 space-y-3">
          {logs.length === 0 ? <EmptyBox>아직 검수 이력이 없습니다.</EmptyBox> : null}
          {logs.map((log) => (
            <article key={log.id} className="rounded-lg border border-border p-4 text-sm">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full bg-neutral-100 px-3 py-1 font-semibold">
                  {VERIFICATION_STATUS_LABELS[log.beforeStatus]}
                </span>
                <span className="text-neutral-400">→</span>
                <span className="rounded-full bg-blue-50 px-3 py-1 font-semibold text-blue-700">
                  {VERIFICATION_STATUS_LABELS[log.afterStatus]}
                </span>
              </div>
              <dl className="mt-3 grid gap-3 md:grid-cols-2">
                <Info label="메모" value={log.memo ?? "-"} wide />
                <Info label="관리자 이메일" value={log.adminEmail ?? "-"} />
                <Info label="검수 시각" value={formatDateTime(log.verifiedAt)} />
              </dl>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}

function BackLink() {
  return (
    <Link className="inline-flex text-sm font-semibold text-accent" href="/admin/benefits">
      ← 혜택 목록으로 돌아가기
    </Link>
  );
}

function Info({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={wide ? "md:col-span-2" : undefined}>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 whitespace-pre-wrap text-neutral-800">{value}</dd>
    </div>
  );
}

function Select({ children, label, onChange, value }: { children: React.ReactNode; label: string; onChange: (value: string) => void; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <select className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required>
        {children}
      </select>
    </label>
  );
}

function TextInput({ label, onChange, required, type = "text", value }: { label: string; onChange: (value: string) => void; required?: boolean; type?: string; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <input className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required={required} type={type} />
    </label>
  );
}

function TextArea({ label, onChange, value }: { label: string; onChange: (value: string) => void; value: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <textarea className="min-h-24 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm leading-6" value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-lg border border-border bg-white p-6 text-center text-sm text-neutral-600">{children}</div>;
}

function Notice({ children, tone }: { children: React.ReactNode; tone: "success" | "error" }) {
  const className =
    tone === "success"
      ? "border-green-200 bg-green-50 text-green-700"
      : "border-red-200 bg-red-50 text-red-700";
  return <div className={`rounded-lg border p-4 text-sm ${className}`}>{children}</div>;
}
