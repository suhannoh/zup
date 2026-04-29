"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  addAdminBenefitTag,
  createAdminBenefitDetailItem,
  createAdminBenefitSource,
  deleteAdminBenefitSource,
  deleteAdminBenefitTag,
  getAdminBenefit,
  getAdminBenefitDetailItems,
  getAdminBenefitSources,
  getAdminBenefitVerificationLogs,
  updateAdminBenefitDetailItem,
  updateAdminBenefitDetailItemActive,
  updateAdminBenefitSource,
  updateAdminBenefitStatus,
} from "@/lib/api/adminApi";
import { getTags } from "@/lib/api/publicApi";
import {
  BENEFIT_TYPE_LABELS,
  BIRTHDAY_TIMING_LABELS,
  OCCASION_TYPE_LABELS,
  VERIFICATION_STATUS_LABELS,
} from "@/lib/benefitLabels";
import { SOURCE_TYPE_LABELS, SOURCE_TYPE_OPTIONS } from "@/lib/sourceLabels";
import type {
  AdminBenefit,
  AdminBenefitDetailItem,
  AdminBenefitDetailItemRequest,
} from "@/types/adminBenefit";
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

type DetailItemFormState = {
  id: number | null;
  brandName: string;
  title: string;
  description: string;
  conditionText: string;
  imageUrl: string;
  displayOrder: number;
  isActive: boolean;
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
    sourceType: source.sourceType ?? "OFFICIAL_HOME",
    sourceUrl: source.sourceUrl ?? "",
    sourceTitle: source.sourceTitle ?? "",
    sourceCheckedAt: source.sourceCheckedAt ?? "",
    memo: source.memo ?? "",
  };
}

function toDetailItemForm(item: AdminBenefitDetailItem): DetailItemFormState {
  return {
    id: item.id,
    brandName: item.brandName ?? "",
    title: item.title ?? "",
    description: item.description ?? "",
    conditionText: item.conditionText ?? "",
    imageUrl: item.imageUrl ?? "",
    displayOrder: item.displayOrder ?? 0,
    isActive: item.isActive ?? true,
  };
}

function toDetailItemRequest(item: DetailItemFormState): AdminBenefitDetailItemRequest {
  return {
    brandName: normalizeOptional(item.brandName),
    title: item.title.trim(),
    description: normalizeOptional(item.description),
    conditionText: normalizeOptional(item.conditionText),
    imageUrl: normalizeOptional(item.imageUrl),
    displayOrder: item.displayOrder,
  };
}

function formatDateTime(value: string) {
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

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function AdminBenefitDetailPanel({ benefitId }: { benefitId: number }) {
  const [benefit, setBenefit] = useState<AdminBenefit | null>(null);
  const [sources, setSources] = useState<AdminBenefitSource[]>([]);
  const [detailItems, setDetailItems] = useState<DetailItemFormState[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [logs, setLogs] = useState<VerificationLog[]>([]);
  const [sourceForm, setSourceForm] = useState<SourceFormState>(emptySourceForm);
  const [editingSource, setEditingSource] = useState<AdminBenefitSource | null>(null);
  const [selectedTagId, setSelectedTagId] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingSource, setSavingSource] = useState(false);
  const [savingDetailItems, setSavingDetailItems] = useState(false);
  const [savingTag, setSavingTag] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const activeSources = sources.filter((source) => source.isActive);
  const activeDetailItems = detailItems.filter((item) => item.isActive && item.title.trim());

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
      const [benefitData, sourceData, tagData, logData, detailItemData] = await Promise.all([
        getAdminBenefit(benefitId),
        getAdminBenefitSources(benefitId),
        getTags(),
        getAdminBenefitVerificationLogs(benefitId),
        getAdminBenefitDetailItems(benefitId),
      ]);
      setBenefit(benefitData);
      setSources(sourceData ?? []);
      setDetailItems((detailItemData ?? []).map(toDetailItemForm));
      setTags(tagData ?? []);
      setLogs(logData ?? []);
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
      setError("출처 처리에 실패했습니다.");
    }
  }

  async function publishBenefit() {
    if (!benefit) {
      return;
    }
    setPublishing(true);
    setMessage(null);
    setError(null);
    try {
      const updated = await updateAdminBenefitStatus(benefitId, {
        verificationStatus: "PUBLISHED",
        lastVerifiedAt: today(),
        memo: "관리자 혜택 상세 화면에서 공개 전환",
      });
      setBenefit(updated);
      await reloadBenefitAndLogs();
      setMessage("혜택이 공개 중 상태로 전환되었습니다.");
    } catch {
      setError("공개 전환에 실패했습니다.");
    } finally {
      setPublishing(false);
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

  function updateDetailItem(index: number, patch: Partial<DetailItemFormState>) {
    setDetailItems((items) => items.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item)));
  }

  function addDetailItem() {
    setDetailItems((items) => [
      ...items,
      { id: null, brandName: "", title: "", description: "", conditionText: "", imageUrl: "", displayOrder: items.length + 1, isActive: true },
    ]);
  }

  function moveDetailItem(index: number, direction: -1 | 1) {
    setDetailItems((items) => {
      const nextIndex = index + direction;
      if (nextIndex < 0 || nextIndex >= items.length) {
        return items;
      }
      const next = [...items];
      [next[index], next[nextIndex]] = [next[nextIndex], next[index]];
      return next.map((item, itemIndex) => ({ ...item, displayOrder: itemIndex + 1 }));
    });
  }

  async function saveDetailItems() {
    setSavingDetailItems(true);
    setMessage(null);
    setError(null);
    try {
      for (const [index, item] of detailItems.entries()) {
        if (!item.title.trim()) {
          continue;
        }
        const next = { ...item, displayOrder: index + 1 };
        if (item.id) {
          await updateAdminBenefitDetailItem(item.id, toDetailItemRequest(next));
        } else {
          await createAdminBenefitDetailItem(benefitId, toDetailItemRequest(next));
        }
      }
      setDetailItems((await getAdminBenefitDetailItems(benefitId)).map(toDetailItemForm));
      setMessage("혜택 상세 리스트가 저장되었습니다.");
    } catch {
      setError("혜택 상세 리스트 저장에 실패했습니다.");
    } finally {
      setSavingDetailItems(false);
    }
  }

  async function deactivateDetailItem(index: number) {
    const item = detailItems[index];
    if (!item.id) {
      setDetailItems((items) => items.filter((_, itemIndex) => itemIndex !== index));
      return;
    }
    setSavingDetailItems(true);
    try {
      await updateAdminBenefitDetailItemActive(item.id, { isActive: false });
      setDetailItems((await getAdminBenefitDetailItems(benefitId)).map(toDetailItemForm));
      setMessage("혜택 상세 항목이 비활성화되었습니다.");
    } catch {
      setError("혜택 상세 항목 비활성화에 실패했습니다.");
    } finally {
      setSavingDetailItems(false);
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

  const publicExposureReady = benefit.verificationStatus === "PUBLISHED" && benefit.isActive && activeSources.length > 0;
  const canPublish = benefit.verificationStatus !== "PUBLISHED" && benefit.isActive && activeSources.length > 0;

  return (
    <section className="space-y-6">
      <div>
        <BackLink />
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-neutral-950">혜택 관리 / 상세 수정</h1>
            <p className="mt-1 text-sm text-neutral-600">승인된 혜택의 상세 리스트, 공식 출처, 공개 상태를 관리합니다.</p>
          </div>
          <StatusPill status={VERIFICATION_STATUS_LABELS[benefit.verificationStatus] ?? benefit.verificationStatus ?? "-"} />
        </div>
      </div>

      {message ? <Notice tone="success">{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">혜택 정보</h2>
            <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
              <Info label="제목" value={benefit.title ?? "-"} />
              <Info label="브랜드" value={benefit.brandName ?? "-"} />
              <Info label="요약" value={benefit.summary ?? "-"} wide />
              <Info label="혜택 유형" value={BENEFIT_TYPE_LABELS[benefit.benefitType] ?? benefit.benefitType ?? "-"} />
              <Info label="적용 시점" value={OCCASION_TYPE_LABELS[benefit.occasionType] ?? benefit.occasionType ?? "-"} />
              <Info label="생일 지급 시점" value={BIRTHDAY_TIMING_LABELS[benefit.birthdayTimingType] ?? benefit.birthdayTimingType ?? "-"} />
              <Info label="상태" value={VERIFICATION_STATUS_LABELS[benefit.verificationStatus] ?? benefit.verificationStatus ?? "-"} />
              <Info label="공개 여부" value={benefit.isActive ? "활성" : "비활성"} />
              <Info label="최종 검증일" value={benefit.lastVerifiedAt ?? "-"} />
              <Info label="사용 가능 기간" value={benefit.usagePeriodDescription ?? "-"} />
              <Info label="구매 조건" value={benefit.conditionSummary ?? "-"} wide />
              <Info label="이용 조건" value={benefit.caution ?? benefit.detail ?? "-"} wide />
            </dl>
            {benefit.verificationStatus === "VERIFIED" ? (
              <p className="mt-4 rounded-lg bg-amber-50 p-3 text-sm leading-6 text-amber-800">
                검증 완료 상태의 혜택은 관리자 검수 완료 상태이며, 사용자 화면에는 노출되지 않습니다. 사용자 화면에 공개하려면 공개 중으로 전환해야 합니다.
              </p>
            ) : null}
          </section>

          <section className="rounded-2xl border-2 border-blue-100 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-bold">혜택 상세 리스트 관리</h2>
                <p className="mt-1 text-sm text-neutral-500">공개 브랜드 페이지의 대표 혜택 목록으로 사용됩니다.</p>
              </div>
              <div className="flex gap-2">
                <button className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={addDetailItem}>
                  항목 추가
                </button>
                <button className="h-9 rounded-lg bg-blue-600 px-3 text-sm font-semibold text-white disabled:opacity-60" type="button" onClick={saveDetailItems} disabled={savingDetailItems}>
                  {savingDetailItems ? "저장 중" : "저장"}
                </button>
              </div>
            </div>
            <div className="mt-4 space-y-3">
              {detailItems.length === 0 ? <EmptyBox>등록된 혜택 상세 항목이 없습니다.</EmptyBox> : null}
              {detailItems.map((item, index) => (
                <div key={item.id ?? `new-${index}`} className={`rounded-xl border border-neutral-200 p-4 ${item.isActive ? "bg-neutral-50" : "bg-neutral-100 opacity-70"}`}>
                  <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                    <span className="text-sm font-semibold">
                      {index + 1}번 항목 {item.isActive ? "" : "(비활성)"}
                    </span>
                    <div className="flex gap-2">
                      <button className="rounded-lg border border-neutral-200 px-2 py-1 text-xs disabled:opacity-50" type="button" onClick={() => moveDetailItem(index, -1)} disabled={index === 0}>위로</button>
                      <button className="rounded-lg border border-neutral-200 px-2 py-1 text-xs disabled:opacity-50" type="button" onClick={() => moveDetailItem(index, 1)} disabled={index === detailItems.length - 1}>아래로</button>
                      <button className="rounded-lg border border-neutral-200 px-2 py-1 text-xs" type="button" onClick={() => deactivateDetailItem(index)}>
                        {item.id ? "비활성화" : "삭제"}
                      </button>
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <TextInput label="브랜드명" value={item.brandName} onChange={(brandName) => updateDetailItem(index, { brandName })} />
                    <TextInput label="혜택명" value={item.title} onChange={(title) => updateDetailItem(index, { title })} required />
                    <TextArea label="설명" value={item.description} onChange={(description) => updateDetailItem(index, { description })} />
                    <TextArea label="사용 조건" value={item.conditionText} onChange={(conditionText) => updateDetailItem(index, { conditionText })} />
                    <div className="md:col-span-2">
                      <ImageUrlField value={item.imageUrl} onChange={(imageUrl) => updateDetailItem(index, { imageUrl })} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">공식 출처 관리</h2>
            <div className="mt-4 grid gap-5 xl:grid-cols-[340px_1fr]">
              <form className="space-y-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4" onSubmit={handleSourceSubmit}>
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-base font-bold">{editingSource ? "출처 수정" : "공식 출처 등록"}</h3>
                  {editingSource ? (
                    <button className="text-sm font-semibold text-blue-600" type="button" onClick={resetSourceForm}>
                      새로 등록
                    </button>
                  ) : null}
                </div>
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
                <button className="h-11 w-full rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={savingSource}>
                  {savingSource ? "저장 중" : editingSource ? "출처 수정" : "출처 등록"}
                </button>
              </form>

              <div className="space-y-3">
                {activeSources.length === 0 ? <EmptyBox>등록된 공식 출처가 없습니다.</EmptyBox> : null}
                {activeSources.map((source) => (
                  <article key={source.id} className="rounded-xl border border-neutral-200 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                          {SOURCE_TYPE_LABELS[source.sourceType]}
                        </span>
                        <h3 className="mt-3 font-semibold">{source.sourceTitle ?? "제목 없음"}</h3>
                      </div>
                      <div className="flex gap-2">
                        <button className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={() => startEditSource(source)}>
                          수정
                        </button>
                        <button className="h-9 rounded-lg border border-neutral-200 px-3 text-sm font-semibold" type="button" onClick={() => handleDeleteSource(source.id)}>
                          비활성화
                        </button>
                      </div>
                    </div>
                    <dl className="mt-3 grid gap-3 text-sm md:grid-cols-2">
                      <div className="md:col-span-2">
                        <dt className="font-medium text-neutral-500">출처 URL</dt>
                        <dd className="mt-1 break-all">
                          <a className="text-blue-600 hover:underline" href={source.sourceUrl} target="_blank" rel="noreferrer">
                            {source.sourceUrl}
                          </a>
                        </dd>
                      </div>
                      <Info label="확인일" value={source.sourceCheckedAt ?? "-"} />
                      <Info label="메모" value={source.memo ?? "-"} />
                    </dl>
                  </article>
                ))}
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">태그 관리</h2>
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
              <select className="h-11 flex-1 rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={selectedTagId} onChange={(event) => setSelectedTagId(event.target.value)}>
                <option value="">태그 선택</option>
                {tags.map((tag) => (
                  <option key={tag.id} value={tag.id} disabled={connectedTagSlugs.has(tag.slug)}>
                    {tag.name}
                  </option>
                ))}
              </select>
              <button className="h-11 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white disabled:opacity-60" type="submit" disabled={savingTag || selectedTagAlreadyConnected}>
                태그 추가
              </button>
            </form>
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">검증 이력</h2>
            <div className="mt-4 space-y-3">
              {logs.length === 0 ? <EmptyBox>아직 검증 이력이 없습니다.</EmptyBox> : null}
              {logs.map((log) => (
                <article key={log.id} className="rounded-lg border border-neutral-200 p-4 text-sm">
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
                    <Info label="검증 시각" value={formatDateTime(log.verifiedAt)} />
                  </dl>
                </article>
              ))}
            </div>
          </section>
        </div>

        <aside className="space-y-5 xl:sticky xl:top-28 xl:self-start">
          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">공개 화면 미리보기</h2>
            <div className="mt-4 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
              <h3 className="text-base font-bold">{benefit.title ?? "-"}</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-700">{benefit.summary ?? "-"}</p>
              {activeDetailItems.length > 0 ? (
                <div className="mt-4">
                  <p className="text-sm font-semibold">대표 혜택</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-neutral-700">
                    {activeDetailItems.slice(0, 5).map((item) => (
                      <li key={item.id ?? item.displayOrder}>
                        - {item.brandName ? `${item.brandName} · ` : ""}{item.title}
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <p className="mt-4 text-sm leading-6 text-neutral-700">{benefit.conditionSummary ?? benefit.caution ?? ""}</p>
            </div>
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">발행 관리</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <Info label="현재 상태" value={VERIFICATION_STATUS_LABELS[benefit.verificationStatus] ?? benefit.verificationStatus ?? "-"} />
              <Info label="공개 여부" value={benefit.isActive ? "활성" : "비활성"} />
              <Info label="공식 출처" value={`${activeSources.length}개`} />
              <Info label="최근 확인일" value={benefit.lastVerifiedAt ?? "-"} />
            </dl>
            <p className="mt-4 rounded-lg bg-neutral-50 p-3 text-xs leading-5 text-neutral-600">
              Public 화면에는 공개 중 상태이면서 활성화된 혜택만 노출됩니다. 공식 출처와 상세 리스트를 확인한 뒤 공개 전환하세요.
            </p>
            <button
              className="mt-4 h-11 w-full rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-60"
              type="button"
              disabled={!canPublish || publishing}
              onClick={publishBenefit}
            >
              {publishing ? "공개 전환 중" : benefit.verificationStatus === "PUBLISHED" ? "이미 공개 중" : "공개 전환"}
            </button>
            {!canPublish && benefit.verificationStatus !== "PUBLISHED" ? (
              <p className="mt-2 text-xs text-neutral-500">활성 혜택과 공식 출처가 있어야 공개 전환할 수 있습니다.</p>
            ) : null}
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <h2 className="text-lg font-bold">공개 노출 체크</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <Info label="상태" value={VERIFICATION_STATUS_LABELS[benefit.verificationStatus] ?? benefit.verificationStatus ?? "-"} />
              <Info label="활성 여부" value={benefit.isActive ? "활성" : "비활성"} />
              <Info label="브랜드 연결" value={`${benefit.brandName ?? "-"} / ${benefit.brandSlug ?? "-"}`} />
              <Info label="공식 출처" value={activeSources.length > 0 ? `${activeSources.length}개 있음` : "없음"} />
              <Info label="상세 혜택" value={`${activeDetailItems.length}개`} />
              <Info label="사용자 페이지 노출" value={publicExposureReady ? "노출 가능" : "노출 전"} />
            </dl>
            <p className={`mt-4 rounded-lg p-3 text-xs leading-5 ${publicExposureReady ? "bg-green-50 text-green-700" : "bg-amber-50 text-amber-800"}`}>
              {publicExposureReady
                ? "현재 혜택은 공개 중 상태이며 사용자 페이지 노출 조건을 만족합니다."
                : benefit.verificationStatus === "VERIFIED"
                  ? "현재 혜택은 검증 완료 상태입니다. 공개 전환 전까지 사용자 페이지에는 표시되지 않습니다."
                  : "사용자 페이지에는 공개 중 상태이고 활성화된 혜택만 표시됩니다."}
            </p>
          </section>
        </aside>
      </section>
    </section>
  );
}

function BackLink() {
  return (
    <Link className="inline-flex text-sm font-semibold text-blue-600 hover:underline" href="/admin/benefits">
      ← 혜택 목록으로 돌아가기
    </Link>
  );
}

function StatusPill({ status }: { status: string }) {
  return <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">{status}</span>;
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
      <select className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} required>
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

function ImageUrlField({ onChange, value }: { onChange: (value: string) => void; value: string }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const imageUrl = value.trim();
  const canPreview = imageUrl.length > 0 && failedUrl !== imageUrl;

  return (
    <div className="space-y-2 text-sm font-medium">
      <span>이미지 URL</span>
      <div className="grid gap-3 rounded-xl border border-neutral-200 bg-white p-3 md:grid-cols-[120px_minmax(0,1fr)]">
        <div className="flex h-24 w-full items-center justify-center overflow-hidden rounded-lg border border-neutral-200 bg-neutral-50">
          {canPreview ? (
            <img
              alt="혜택 이미지 미리보기"
              className="max-h-full max-w-full object-contain"
              src={imageUrl}
              onError={() => setFailedUrl(imageUrl)}
              onLoad={() => setFailedUrl(null)}
            />
          ) : imageUrl ? (
            <span className="px-3 text-center text-xs leading-5 text-neutral-500">미리보기 실패</span>
          ) : (
            <span className="px-3 text-center text-xs leading-5 text-neutral-400">이미지 없음</span>
          )}
        </div>
        <div className="min-w-0 space-y-2">
          <input
            className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm"
            value={value}
            onChange={(event) => {
              setFailedUrl(null);
              onChange(event.target.value);
            }}
            placeholder="https://..."
            type="url"
          />
          <div className="flex flex-wrap items-center gap-2">
            {imageUrl ? (
              <a
                className="rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-semibold text-neutral-700 hover:border-blue-200 hover:text-blue-700"
                href={imageUrl}
                target="_blank"
                rel="noreferrer"
              >
                새 탭에서 보기
              </a>
            ) : null}
            {failedUrl === imageUrl && imageUrl ? (
              <span className="text-xs text-red-600">이미지를 불러올 수 없습니다.</span>
            ) : (
              <span className="text-xs text-neutral-500">검수용 미리보기이며 저장 값은 URL 문자열입니다.</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-xl border border-neutral-200 bg-white p-6 text-center text-sm text-neutral-600">{children}</div>;
}

function Notice({ children, tone }: { children: React.ReactNode; tone: "success" | "error" }) {
  const className =
    tone === "success"
      ? "border-green-200 bg-green-50 text-green-700"
      : "border-red-200 bg-red-50 text-red-700";
  return <div className={`rounded-2xl border p-4 text-sm ${className}`}>{children}</div>;
}
