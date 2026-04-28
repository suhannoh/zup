import { AdminSourceWatchesPanel } from "@/components/admin/AdminSourceWatchesPanel";

export default function AdminSourceWatchesPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">공식 출처 수집 관리</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          브랜드 공식 URL을 등록하고, 수동 수집을 실행해 혜택 후보를 관리자 검수 큐로 보냅니다.
        </p>
      </section>
      <AdminSourceWatchesPanel />
    </div>
  );
}
