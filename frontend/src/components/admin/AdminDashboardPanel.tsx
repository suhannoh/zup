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
