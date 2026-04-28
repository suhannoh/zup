import { AdminDashboardPanel } from "@/components/admin/AdminDashboardPanel";

export default function AdminPage() {
  return (
    <div className="space-y-8">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-blue-600">Zup admin</p>
        <h1 className="text-3xl font-bold text-neutral-950">관리자 대시보드</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          공식 출처 자동 수집 및 혜택 운영 현황을 한눈에 확인하세요.
        </p>
      </section>

      <AdminDashboardPanel />
    </div>
  );
}
