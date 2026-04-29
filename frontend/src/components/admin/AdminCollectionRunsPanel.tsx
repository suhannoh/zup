"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { getCollectionRuns } from "@/lib/api/adminApi";
import {
  COLLECTION_FAILURE_REASON_LABELS,
  COLLECTION_STATUS_CLASS,
  COLLECTION_STATUS_LABELS,
  TRIGGER_TYPE_LABELS,
} from "@/lib/adminLabels";
import type { PageResponse } from "@/types/api";
import type { CollectionRun, CollectionRunSearchParams, CollectionRunStatus } from "@/types/collectionRun";

const failureReasonOptions = [
  { value: "ALL", label: "전체" },
  { value: "ROBOTS_TXT_DISALLOWED", label: "robots.txt 차단" },
  { value: "ROBOTS_TXT_FETCH_FAILED", label: "robots.txt 조회 실패" },
  { value: "RATE_LIMITED_BY_DOMAIN", label: "도메인 최소 수집 간격 미도달" },
  { value: "COLLECTION_ALREADY_RUNNING", label: "이미 수집 중" },
  { value: "FETCH_FAILED", label: "HTML fetch 실패" },
  { value: "EXTRACT_FAILED", label: "파싱 실패" },
];

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
    second: "2-digit",
  }).format(new Date(value));
}

function shortText(value: string | null, maxLength = 180) {
  if (!value) {
    return "-";
  }
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
}

export function AdminCollectionRunsPanel() {
  const searchParams = useSearchParams();
  const [runs, setRuns] = useState<CollectionRun[]>([]);
  const [pageData, setPageData] = useState<PageResponse<CollectionRun> | null>(null);
  const [statusFilter, setStatusFilter] = useState<CollectionRunStatus | "ALL">(
    (searchParams.get("status") as CollectionRunStatus | "ALL") ?? "ALL"
  );
  const [failureReasonFilter, setFailureReasonFilter] = useState(searchParams.get("failureReason") ?? "ALL");
  const [sourceWatchIdFilter, setSourceWatchIdFilter] = useState(searchParams.get("sourceWatchId") ?? "");
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const debouncedSourceWatchIdFilter = useDebouncedValue(sourceWatchIdFilter, 300);
  const debouncedKeyword = useDebouncedValue(keyword, 300);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(Number(searchParams.get("page") ?? 0) || 0);
  const pageSize = 20;

  const params = useMemo<CollectionRunSearchParams>(
    () => ({
      status: statusFilter === "ALL" ? undefined : statusFilter,
      failureReason: failureReasonFilter === "ALL" ? undefined : failureReasonFilter,
      sourceWatchId: debouncedSourceWatchIdFilter.trim() ? Number(debouncedSourceWatchIdFilter) : undefined,
      keyword: debouncedKeyword.trim() || undefined,
      page,
      size: pageSize,
    }),
    [debouncedKeyword, debouncedSourceWatchIdFilter, failureReasonFilter, page, statusFilter]
  );

  useEffect(() => {
    setPage(0);
  }, [debouncedKeyword, debouncedSourceWatchIdFilter, failureReasonFilter, statusFilter]);

  useEffect(() => {
    let active = true;

    async function loadRuns() {
      setLoading(true);
      setError(null);
      try {
        const data = await getCollectionRuns(params);
        if (active) {
          setPageData(data);
          setRuns(data.items);
        }
      } catch {
        if (active) {
          setError("수집 실행 이력을 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadRuns();

    return () => {
      active = false;
    };
  }, [params]);

  return (
    <section className="space-y-4">
      <div className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
        <div className="grid gap-3 lg:grid-cols-4">
          <FieldSelect label="전체 상태" value={statusFilter} onChange={(value) => setStatusFilter(value as CollectionRunStatus | "ALL")}>
            <option value="ALL">전체</option>
            <option value="SUCCESS">성공</option>
            <option value="FAILED">실패</option>
            <option value="SKIPPED">건너뜀</option>
          </FieldSelect>
          <FieldSelect label="실패/스킵 사유" value={failureReasonFilter} onChange={setFailureReasonFilter}>
            {failureReasonOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </FieldSelect>
          <FieldInput label="SourceWatch ID" value={sourceWatchIdFilter} onChange={setSourceWatchIdFilter} placeholder="예: 4" />
          <FieldInput label="검색" value={keyword} onChange={setKeyword} placeholder="출처명 / 브랜드명 / URL 검색" />
        </div>
      </div>

      {error ? <Notice>{error}</Notice> : null}
      {keyword !== debouncedKeyword || sourceWatchIdFilter !== debouncedSourceWatchIdFilter ? (
        <Notice>필터를 적용하는 중입니다.</Notice>
      ) : null}
      {loading ? <EmptyBox>수집 실행 이력을 불러오는 중입니다.</EmptyBox> : null}
      {!loading && runs.length === 0 ? <EmptyBox>아직 수집 실행 이력이 없습니다.</EmptyBox> : null}

      {runs.map((run) => (
        <article key={run.id} className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-lg font-semibold">{run.sourceWatchTitle}</h2>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${COLLECTION_STATUS_CLASS[run.status]}`}>
                  {COLLECTION_STATUS_LABELS[run.status] ?? run.status}
                </span>
                <span className="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-700">
                  {TRIGGER_TYPE_LABELS[run.triggerType] ?? run.triggerType}
                </span>
                {run.failureReason ? (
                  <span className="rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-700">
                    {COLLECTION_FAILURE_REASON_LABELS[run.failureReason] ?? run.failureReason}
                  </span>
                ) : null}
              </div>
              <p className="mt-1 text-sm text-neutral-500">
                {run.brandName} · 수집 시각 {formatDateTime(run.startedAt)}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Link className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold" href={`/admin/source-watches?sourceWatchId=${run.sourceWatchId}`}>
                SourceWatch 보기
              </Link>
              {run.candidateCount > 0 ? (
                <Link
                  className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold"
                  href={
                    run.id
                      ? `/admin/benefit-candidates?collectionRunId=${run.id}`
                      : `/admin/benefit-candidates?sourceWatchId=${run.sourceWatchId}`
                  }
                >
                  생성된 후보 보기
                </Link>
              ) : null}
            </div>
          </div>

          <dl className="mt-4 grid gap-3 text-sm md:grid-cols-3">
            <Info label="출처명" value={run.sourceWatchTitle} />
            <Info label="브랜드명" value={run.brandName} />
            <Info label="후보 생성 수" value={`${run.candidateCount}개`} />
            <Info label="snapshotId" value={run.snapshotId === null ? "-" : String(run.snapshotId)} />
            <Info label="sameAsPrevious" value={run.fetched ? (run.sameAsPrevious ? "동일 본문" : "새 본문") : "-"} />
            <Info label="수집 종료 시각" value={formatDateTime(run.finishedAt)} />
            <Info label="실패/스킵 사유" value={run.failureReason ? (COLLECTION_FAILURE_REASON_LABELS[run.failureReason] ?? run.failureReason) : "-"} />
            <Info label="fetched 여부" value={run.fetched ? "예" : "아니오"} />
            <Info label="소요 시간" value={run.durationMillis === null ? "-" : `${run.durationMillis}ms`} />
            <Info label="URL" value={run.sourceWatchUrl} wide />
            <Info label="결과 요약" value={run.message} wide />
            <Info label="상세 원인" value={shortText(run.detailReason)} wide />
          </dl>
        </article>
      ))}
      <PaginationBar
        disabled={loading}
        pageData={pageData}
        onNext={() => setPage((current) => current + 1)}
        onPrevious={() => setPage((current) => Math.max(0, current - 1))}
      />
    </section>
  );
}

function PaginationBar({
  disabled,
  onNext,
  onPrevious,
  pageData,
}: {
  disabled: boolean;
  onNext: () => void;
  onPrevious: () => void;
  pageData: PageResponse<unknown> | null;
}) {
  const currentPage = (pageData?.page ?? 0) + 1;
  const totalPages = Math.max(pageData?.totalPages ?? 0, 1);
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-white p-4 text-sm">
      <span className="font-medium text-neutral-700">
        총 {pageData?.totalElements ?? 0}개 · {currentPage} / {totalPages} 페이지
      </span>
      <div className="flex gap-2">
        <button
          className="h-9 rounded-lg border border-border px-3 text-sm font-semibold disabled:opacity-50"
          type="button"
          onClick={onPrevious}
          disabled={disabled || !pageData?.hasPrevious}
        >
          이전
        </button>
        <button
          className="h-9 rounded-lg border border-border px-3 text-sm font-semibold disabled:opacity-50"
          type="button"
          onClick={onNext}
          disabled={disabled || !pageData?.hasNext}
        >
          다음
        </button>
      </div>
    </div>
  );
}

function FieldSelect({
  children,
  label,
  onChange,
  value,
}: {
  children: React.ReactNode;
  label: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <select className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}>
        {children}
      </select>
    </label>
  );
}

function FieldInput({
  label,
  onChange,
  placeholder,
  value,
}: {
  label: string;
  onChange: (value: string) => void;
  placeholder?: string;
  value: string;
}) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <input className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    </label>
  );
}

function Info({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={wide ? "md:col-span-3" : undefined}>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 break-words text-neutral-800">{value}</dd>
    </div>
  );
}

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">{children}</div>;
}

function Notice({ children }: { children: React.ReactNode }) {
  return <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{children}</div>;
}
