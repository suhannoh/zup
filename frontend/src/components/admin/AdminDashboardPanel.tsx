"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getAdminDashboard } from "@/lib/api/adminApi";
import type { AdminDashboard } from "@/types/report";

type DashboardCard = {
  label: string;
  value: number;
  tone?: "default" | "warning" | "success";
  description?: string;
};

function DashboardMetricCard({ description, label, value, tone = "default" }: DashboardCard) {
  const toneClass = {
    default: "text-neutral-950",
    warning: "text-amber-700",
    success: "text-green-700",
  }[tone];

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-neutral-500">{label}</p>
      <p className={`mt-3 text-3xl font-bold ${toneClass}`}>{value.toLocaleString()}</p>
      {description ? <p className="mt-2 text-xs text-neutral-500">{description}</p> : null}
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

  const operationCards: DashboardCard[] = dashboard
    ? [
        { label: "전체 수집 URL", value: dashboard.collectionSummary.totalSourceWatchCount },
        { label: "활성 수집 URL", value: dashboard.collectionSummary.activeSourceWatchCount },
        { label: "검수 대기 후보", value: dashboard.collectionSummary.pendingCandidateCount, tone: "warning" },
        { label: "최근 성공", value: dashboard.collectionSummary.recentSuccessRunCount, tone: "success" },
        { label: "최근 실패", value: dashboard.collectionSummary.recentFailedRunCount, tone: "warning" },
        { label: "최근 스킵", value: dashboard.collectionSummary.recentSkippedRunCount },
      ]
    : [];

  return (
    <div className="space-y-8">
      {loading ? (
        <section className="rounded-2xl border border-neutral-200 bg-white p-5 text-sm text-neutral-600 shadow-sm">
          대시보드를 불러오는 중입니다.
        </section>
      ) : null}

      {error ? (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
          {error}
        </section>
      ) : null}

      {dashboard ? (
        <>
          <section>
            <div className="mb-4">
              <h2 className="text-lg font-bold text-neutral-950">수집 운영 현황</h2>
              <p className="mt-1 text-sm text-neutral-500">공식 출처 수집과 후보 검수 상태를 빠르게 확인합니다.</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
              {operationCards.map((card) => (
                <DashboardMetricCard key={card.label} {...card} />
              ))}
            </div>
          </section>

          <section className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-bold">빠른 작업</h2>
            <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <QuickAction href="/admin/benefit-candidates" title="혜택 후보 검수" description="검수 대기 후보를 확인하고 승인하세요." />
              <QuickAction href="/admin/benefits" title="혜택 관리" description="승인된 혜택을 수정하고 공개 상태를 관리하세요." />
              <QuickAction href="/admin/source-watches" title="공식 출처 수집 관리" description="수집 URL을 등록하고 수집 실행을 관리하세요." />
              <QuickAction href="/admin/collection-runs" title="수집 실행 이력" description="수집 성공, 실패, 스킵 이력을 확인하세요." />
            </div>
          </section>

          <section className="grid gap-5 xl:grid-cols-2">
            <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
              <h2 className="text-lg font-bold">오늘 확인할 항목</h2>
              {dashboard.collectionSummary.pendingCandidateCount > 0 ? (
                <p className="mt-3 text-sm leading-6 text-neutral-700">
                  검수 대기 후보 {dashboard.collectionSummary.pendingCandidateCount.toLocaleString()}개가 있습니다.
                  <Link className="ml-2 font-semibold text-blue-600 hover:underline" href="/admin/benefit-candidates">
                    검수하러 가기
                  </Link>
                </p>
              ) : (
                <p className="mt-3 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">
                  오늘 확인할 검수 대기 후보가 없습니다.
                </p>
              )}
            </div>

            <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
              <h2 className="text-lg font-bold">최근 수집 실행 현황</h2>
              {dashboard.collectionSummary.recentFailedRuns.length > 0 ? (
                <div className="mt-3 overflow-hidden rounded-lg border border-neutral-200">
                  {dashboard.collectionSummary.recentFailedRuns.slice(0, 4).map((run) => (
                    <div key={run.runId} className="grid gap-2 border-b border-neutral-100 p-3 text-sm last:border-b-0 md:grid-cols-[1fr_auto]">
                      <div>
                        <p className="font-semibold">{run.sourceWatchTitle}</p>
                        <p className="text-xs text-neutral-500">
                          {run.brandName} · {formatDateTime(run.startedAt)}
                        </p>
                      </div>
                      <span className="w-fit rounded-full bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">
                        {run.failureReason ?? "실패"}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="mt-3 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">
                  최근 수집 실행 이력이 없습니다.
                </p>
              )}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}

function QuickAction({ description, href, title }: { description: string; href: string; title: string }) {
  return (
    <Link className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm hover:border-blue-300 hover:bg-blue-50" href={href}>
      <h3 className="text-base font-bold text-neutral-950">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-neutral-600">{description}</p>
    </Link>
  );
}
