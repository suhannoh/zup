import Link from "next/link";
import { getAdminDashboard } from "@/lib/api/adminApi";
import type { AdminDashboard } from "@/types/report";

export const dynamic = "force-dynamic";

type DashboardCard = {
  label: string;
  value: number;
  tone?: "default" | "warning" | "success";
};

async function safeDashboard(): Promise<AdminDashboard | null> {
  try {
    return await getAdminDashboard();
  } catch {
    return null;
  }
}

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

export default async function AdminPage() {
  const dashboard = await safeDashboard();

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
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">관리자 대시보드</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          제보와 검수 상태를 확인하고 운영에 필요한 작업 화면으로 이동합니다.
        </p>
      </section>

      {dashboard ? (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {cards.map((card) => (
            <DashboardMetricCard key={card.label} {...card} />
          ))}
        </section>
      ) : (
        <section className="rounded-lg border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
          대시보드 정보를 불러오지 못했습니다. 백엔드 실행 상태와 API 주소를 확인해 주세요.
        </section>
      )}

      <section className="rounded-lg border border-border bg-white p-5">
        <h2 className="text-lg font-semibold">관리자 메뉴</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/reports">
            제보 관리
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/brands">
            브랜드 관리
          </Link>
          <Link className="rounded-lg border border-border p-4 text-sm font-semibold hover:border-accent" href="/admin/benefits">
            혜택 관리
          </Link>
        </div>
      </section>
    </div>
  );
}
