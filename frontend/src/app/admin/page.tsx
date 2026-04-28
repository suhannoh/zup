import { AdminDashboardPanel } from "@/components/admin/AdminDashboardPanel";

export default function AdminPage() {
  return (
    <div className="space-y-8">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">관리자 대시보드</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          제보와 검수 상태를 확인하고 운영에 필요한 작업 화면으로 이동합니다.
        </p>
      </section>

      <AdminDashboardPanel />
    </div>
  );
}
