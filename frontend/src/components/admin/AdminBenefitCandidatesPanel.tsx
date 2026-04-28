"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
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
  const [candidates, setCandidates] = useState<BenefitCandidate[]>([]);
  const [statusFilter, setStatusFilter] = useState<BenefitCandidateStatus | "ALL">("NEEDS_REVIEW");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadCandidates() {
      setLoading(true);
      setError(null);
      try {
        const data = await getBenefitCandidates();
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
  }, []);

  const filteredCandidates = useMemo(
    () => candidates.filter((candidate) => statusFilter === "ALL" || candidate.status === statusFilter),
    [candidates, statusFilter]
  );

  return (
    <section className="space-y-5">
      <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
        <label className="block max-w-xs space-y-2 text-sm font-semibold">
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
      </div>

      {error ? <Notice>{error}</Notice> : null}
      {loading ? <EmptyBox>혜택 후보 목록을 불러오는 중입니다.</EmptyBox> : null}
      {!loading && filteredCandidates.length === 0 ? <EmptyBox>표시할 혜택 후보가 없습니다.</EmptyBox> : null}

      <div className="space-y-4">
        {filteredCandidates.map((candidate) => (
          <article key={candidate.id} className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-bold">{candidate.title}</h2>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${CANDIDATE_STATUS_CLASS[candidate.status]}`}>
                    {CANDIDATE_STATUS_LABELS[candidate.status]}
                  </span>
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
            <dl className="mt-4 grid gap-3 text-sm md:grid-cols-4">
              <Info label="수집 출처 ID" value={String(candidate.sourceWatchId)} />
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
