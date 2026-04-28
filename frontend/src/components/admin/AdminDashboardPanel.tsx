"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getAdminDashboard } from "@/lib/api/adminApi";
import type { AdminDashboard } from "@/types/report";

type DashboardCard = {
  label: string;
  value: number;
  tone?: "default" | "warning" | "success";
};

function DashboardMetricCard({ label, value, tone = "default" }: DashboardCard) {
  const toneClass = {
    default: "text-neutral-950",
    warning: "text-amber-700",
    success: "text-green-700",
  }[tone];

  return (
    <div className="rounded-lg border border-border bg-white p-5">
      <p className="text-sm font-medium text-neutral-500">{label}</p>
      <p className={`mt-3 text-3xl font-bold ${toneClass}`}>{value.toLocaleString()}</p>
    </div>
  );
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

export function AdminDashboardPanel() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadDashboard() {
      setLoading(true);
      setError(null);

      try {
        const data = await getAdminDashboard();
        if (active) {
          setDashboard(data);
        }
      } catch {
        if (active) {
          setError("대시보드 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      active = false;
    };
  }, []);

  const cards: DashboardCard[] = dashboard
    ? [
        { label: "전체 브랜드 수", value: dashboard.brandCount },
        { label: "게시된 혜택 수", value: dashboard.publishedBenefitCount, tone: "success" },
        { label: "검수 필요 혜택 수", value: dashboard.needsCheckBenefitCount, tone: "warning" },
        { label: "오래된 혜택 수", value: dashboard.staleBenefitCount, tone: "warning" },
        { label: "접수된 제보 수", value: dashboard.receivedReportCount },
        { label: "검토 중 제보 수", value: dashboard.reviewingReportCount, tone: "warning" },
        { label: "처리 완료 제보 수", value: dashboard.resolvedReportCount, tone: "success" },
      ]
    : [];

  return (
    <div className="space-y-8">
      {loading ? (
        <section className="rounded-lg border border-border bg-white p-5 text-sm text-neutral-600">
          대시보드를 불러오는 중입니다.
        </section>
      ) : null}

      {error ? (
        <section className="rounded-lg border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
          {error}
        </section>
      ) : null}

      {dashboard ? (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {cards.map((card) => (
            <DashboardMetricCard key={card.label} {...card} />
          ))}
        </section>
      ) : null}

      {dashboard ? (
        <section className="space-y-5 rounded-lg border border-border bg-white p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold">공식 출처 자동 수집 운영 현황</h2>
              <p className="mt-1 text-sm text-neutral-500">
                최근 수집 기준은 최근 24시간입니다.
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Link className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold" href="/admin/collection-runs">
                수집 실행 이력 보기
              </Link>
              <Link className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold" href="/admin/source-watches">
                공식 출처 수집 관리
              </Link>
              <Link className="h-9 rounded-lg border border-border px-3 py-2 text-sm font-semibold" href="/admin/benefit-candidates">
                혜택 후보 검수
              </Link>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
            <DashboardMetricCard label="전체 수집 URL" value={dashboard.collectionSummary.totalSourceWatchCount} />
            <DashboardMetricCard label="활성 수집 URL" value={dashboard.collectionSummary.activeSourceWatchCount} />
            <DashboardMetricCard label="검수 대기 후보" value={dashboard.collectionSummary.pendingCandidateCount} tone="warning" />
            <DashboardMetricCard label="최근 성공" value={dashboard.collectionSummary.recentSuccessRunCount} tone="success" />
            <DashboardMetricCard label="최근 실패" value={dashboard.collectionSummary.recentFailedRunCount} tone="warning" />
            <DashboardMetricCard label="최근 스킵" value={dashboard.collectionSummary.recentSkippedRunCount} />
          </div>

          <div>
            <h3 className="text-sm font-semibold text-neutral-700">최근 실패 SourceWatch</h3>
            {dashboard.collectionSummary.recentFailedRuns.length === 0 ? (
              <p className="mt-3 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">
                최근 수집 실패가 없습니다.
              </p>
            ) : (
              <div className="mt-3 space-y-3">
                {dashboard.collectionSummary.recentFailedRuns.map((run) => (
                  <article key={run.runId} className="rounded-lg border border-border p-4 text-sm">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="font-semibold">{run.sourceWatchTitle}</p>
                        <p className="mt-1 text-neutral-500">{run.brandName}</p>
                      </div>
                      <span className="rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-700">
                        {run.failureReason ?? "FAILED"}
                      </span>
                    </div>
                    <p className="mt-3 text-neutral-700">{run.errorMessage ?? "-"}</p>
                    <p className="mt-2 text-xs text-neutral-500">실패 시간: {formatDateTime(run.startedAt)}</p>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      ) : null}

      <section className="rounded-lg border border-border bg-white p-5">
        <h2 className="text-lg font-semibold">관리자 메뉴</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/reports">
            제보 관리
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/brands">
            브랜드 관리
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/benefits">
            혜택 관리
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/source-watches">
            공식 출처 수집 관리
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/benefit-candidates">
            혜택 후보 검수
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/collection-runs">
            수집 실행 이력
            <span className="mt-2 block text-xs font-normal text-neutral-500">
              공식 출처 수집의 성공/실패/스킵 기록
            </span>
          </Link>
        </div>
      </section>
    </div>
  );
}
