"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getCollectionRuns } from "@/lib/api/adminApi";
import type { CollectionRun, CollectionRunStatus } from "@/types/collectionRun";

const statusClass: Record<CollectionRunStatus, string> = {
  RUNNING: "bg-blue-50 text-blue-700",
  SUCCESS: "bg-green-50 text-green-700",
  FAILED: "bg-red-50 text-red-700",
  SKIPPED: "bg-amber-50 text-amber-700",
};

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
  const [runs, setRuns] = useState<CollectionRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadRuns() {
      setLoading(true);
      setError(null);
      try {
        const data = await getCollectionRuns();
        if (active) {
          setRuns(data);
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
  }, []);

  return (
    <section className="space-y-4">
      {error ? <Notice>{error}</Notice> : null}
      {loading ? <EmptyBox>수집 실행 이력을 불러오는 중입니다.</EmptyBox> : null}
      {!loading && runs.length === 0 ? <EmptyBox>아직 수집 실행 이력이 없습니다.</EmptyBox> : null}

      {runs.map((run) => (
        <article key={run.id} className="rounded-lg border border-border bg-white p-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-lg font-semibold">{run.sourceWatchTitle}</h2>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClass[run.status]}`}>
                  {run.status}
                </span>
                <span className="rounded-full bg-neutral-100 px-3 py-1 text-xs font-semibold text-neutral-700">
                  {run.triggerType}
                </span>
              </div>
              <p className="mt-1 text-sm text-neutral-500">
                {run.brandName} · startedAt {formatDateTime(run.startedAt)}
              </p>
            </div>
            <Link className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold" href="/admin/source-watches">
              SourceWatch로 이동
            </Link>
          </div>

          <dl className="mt-4 grid gap-3 text-sm md:grid-cols-3">
            <Info label="durationMillis" value={run.durationMillis === null ? "-" : `${run.durationMillis}ms`} />
            <Info label="candidateCount" value={String(run.candidateCount)} />
            <Info label="sameAsPrevious" value={String(run.sameAsPrevious)} />
            <Info label="fetched" value={String(run.fetched)} />
            <Info label="failureReason" value={run.failureReason ?? "-"} />
            <Info label="endedAt" value={formatDateTime(run.endedAt)} />
            <Info label="sourceWatchUrl" value={run.sourceWatchUrl} wide />
            <Info label="errorMessage" value={shortText(run.errorMessage)} wide />
          </dl>
        </article>
      ))}
    </section>
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
