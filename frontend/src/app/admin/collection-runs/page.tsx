import { AdminCollectionRunsPanel } from "@/components/admin/AdminCollectionRunsPanel";

export default function AdminCollectionRunsPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">수집 실행 이력</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          공식 출처 자동 수집의 성공, 실패, 스킵 기록과 후보 생성 결과를 확인합니다.
        </p>
      </section>
      <AdminCollectionRunsPanel />
    </div>
  );
}
