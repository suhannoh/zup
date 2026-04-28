import { AdminReportsPanel } from "@/components/admin/AdminReportsPanel";

export default function AdminReportsPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">제보 관리</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          사용자가 제보한 잘못된 정보, 종료된 혜택, 새 공식 링크를 확인하고 처리 상태를 관리합니다.
        </p>
      </section>
      <AdminReportsPanel />
    </div>
  );
}
