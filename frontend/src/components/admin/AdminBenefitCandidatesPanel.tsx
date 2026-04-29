"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { getBenefitCandidates } from "@/lib/api/adminApi";
import { CANDIDATE_STATUS_CLASS, CANDIDATE_STATUS_LABELS } from "@/lib/adminLabels";
import type { BenefitCandidate, BenefitCandidateStatus } from "@/types/benefitCandidate";

const statusOptions: Array<BenefitCandidateStatus | "ALL"> = ["ALL", "NEEDS_REVIEW", "APPROVED", "REJECTED"];

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

export function AdminBenefitCandidatesPanel() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [candidates, setCandidates] = useState<BenefitCandidate[]>([]);
  const [statusFilter, setStatusFilter] = useState<BenefitCandidateStatus | "ALL">(
    (searchParams.get("status") as BenefitCandidateStatus | "ALL") ?? "NEEDS_REVIEW"
  );
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [sourceWatchIdFilter, setSourceWatchIdFilter] = useState(searchParams.get("sourceWatchId") ?? "");
  const [collectionRunIdFilter, setCollectionRunIdFilter] = useState(searchParams.get("collectionRunId") ?? "");
  const debouncedKeyword = useDebouncedValue(keyword, 300);
  const debouncedSourceWatchIdFilter = useDebouncedValue(sourceWatchIdFilter, 300);
  const debouncedCollectionRunIdFilter = useDebouncedValue(collectionRunIdFilter, 300);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStatusFilter((searchParams.get("status") as BenefitCandidateStatus | "ALL") ?? "NEEDS_REVIEW");
    setKeyword(searchParams.get("keyword") ?? "");
    setSourceWatchIdFilter(searchParams.get("sourceWatchId") ?? "");
    setCollectionRunIdFilter(searchParams.get("collectionRunId") ?? "");
  }, [searchParams]);

  const params = useMemo(
    () => ({
      sourceWatchId: debouncedSourceWatchIdFilter.trim() ? Number(debouncedSourceWatchIdFilter) : undefined,
      collectionRunId: debouncedCollectionRunIdFilter.trim() ? Number(debouncedCollectionRunIdFilter) : undefined,
      status: statusFilter === "ALL" ? undefined : statusFilter,
      keyword: debouncedKeyword.trim() || undefined,
      limit: 100,
    }),
    [debouncedCollectionRunIdFilter, debouncedKeyword, debouncedSourceWatchIdFilter, statusFilter]
  );

  useEffect(() => {
    let active = true;

    async function loadCandidates() {
      setLoading(true);
      setError(null);
      try {
        const data = await getBenefitCandidates(params);
        if (active) {
          setCandidates(data);
        }
      } catch {
        if (active) {
          setError("혜택 후보 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadCandidates();

    return () => {
      active = false;
    };
  }, [params]);

  const trackingFilterLabel = collectionRunIdFilter.trim()
    ? `CollectionRun #${collectionRunIdFilter}에서 생성된 후보만 보는 중`
    : sourceWatchIdFilter.trim()
      ? `SourceWatch #${sourceWatchIdFilter}에서 생성된 후보만 보는 중`
      : null;

  return (
    <section className="space-y-5">
      <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
        <div className="grid gap-3 lg:grid-cols-4">
          <label className="space-y-2 text-sm font-semibold">
            <span>상태 필터</span>
            <select
              className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as BenefitCandidateStatus | "ALL")}
            >
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {status === "ALL" ? "전체" : CANDIDATE_STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </label>
          <FieldInput label="검색" value={keyword} onChange={setKeyword} placeholder="브랜드명 / 후보명 / 출처명 검색" />
          <FieldInput label="SourceWatch ID" value={sourceWatchIdFilter} onChange={setSourceWatchIdFilter} placeholder="예: 4" />
          <FieldInput label="CollectionRun ID" value={collectionRunIdFilter} onChange={setCollectionRunIdFilter} placeholder="예: 12" />
        </div>
        {trackingFilterLabel ? (
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
              {trackingFilterLabel}
            </span>
            <button
              className="rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-semibold text-neutral-700"
              type="button"
              onClick={() => router.push("/admin/benefit-candidates")}
            >
              필터 해제
            </button>
          </div>
        ) : null}
      </div>

      {error ? <Notice>{error}</Notice> : null}
      {(keyword !== debouncedKeyword
        || sourceWatchIdFilter !== debouncedSourceWatchIdFilter
        || collectionRunIdFilter !== debouncedCollectionRunIdFilter) ? (
        <Notice>필터를 적용하는 중입니다.</Notice>
      ) : null}
      {loading ? <EmptyBox>혜택 후보 목록을 불러오는 중입니다.</EmptyBox> : null}
      {!loading && candidates.length === 0 ? <EmptyBox>표시할 혜택 후보가 없습니다.</EmptyBox> : null}

      <div className="space-y-4">
        {candidates.map((candidate) => (
          <article key={candidate.id} className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-bold">{candidate.title}</h2>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${CANDIDATE_STATUS_CLASS[candidate.status]}`}>
                    {CANDIDATE_STATUS_LABELS[candidate.status]}
                  </span>
                  {candidate.needsManualReview ? (
                    <span className="rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-700">
                      수동 재검토 필요
                    </span>
                  ) : null}
                </div>
                <p className="mt-1 text-sm text-neutral-500">
                  {candidate.brandName} · 신뢰도 {Number(candidate.confidence).toFixed(2)}
                </p>
              </div>
              <Link className="h-10 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white" href={`/admin/benefit-candidates/${candidate.id}`}>
                검수하기
              </Link>
            </div>
            <p className="mt-4 text-sm leading-6 text-neutral-700">{candidate.summary}</p>
            {candidate.needsManualReview ? (
              <p className="mt-3 rounded-lg bg-amber-50 p-3 text-sm leading-6 text-amber-800">
                이 후보는 현재 자동 수집이 차단된 출처에서 생성된 항목입니다. 공식 페이지를 직접 확인한 뒤 내용을 검수하거나 반려해 주세요.
              </p>
            ) : null}
            <dl className="mt-4 grid gap-3 text-sm md:grid-cols-4">
              <Info label="수집 출처명" value={candidate.sourceWatchTitle} />
              <Info label="SourceWatch ID" value={String(candidate.sourceWatchId)} />
              <Info label="CollectionRun ID" value={candidate.collectionRunId ? String(candidate.collectionRunId) : "-"} />
              <Info label="Snapshot ID" value={String(candidate.snapshotId)} />
              <Info label="생성 일시" value={formatDateTime(candidate.createdAt)} />
              <Info label="생성된 혜택 ID" value={candidate.approvedBenefitId ? String(candidate.approvedBenefitId) : "-"} />
              <Info label="승인 일시" value={formatDateTime(candidate.approvedAt)} />
            </dl>
          </article>
        ))}
      </div>
    </section>
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
    <label className="space-y-2 text-sm font-semibold">
      <span>{label}</span>
      <input
        className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </label>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 break-all text-neutral-800">{value}</dd>
    </div>
  );
}

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-600 shadow-sm">{children}</div>;
}

function Notice({ children }: { children: React.ReactNode }) {
  return <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{children}</div>;
}
