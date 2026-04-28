import { AdminSourceWatchesPanel } from "@/components/admin/AdminSourceWatchesPanel";

export default function AdminSourceWatchesPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-blue-600">Zup admin</p>
        <h1 className="text-3xl font-bold text-neutral-950">공식 출처 수집 관리</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          브랜드 공식 URL을 등록하고, 수집 실행과 저장된 스냅샷 기반 후보 재생성을 관리합니다.
        </p>
      </section>
      <AdminSourceWatchesPanel />
    </div>
  );
}
