import { AdminBenefitsPanel } from "@/components/admin/AdminBenefitsPanel";

export default function AdminBenefitsPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">혜택 관리</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          브랜드별 생일 혜택 정보를 등록하고, 공식 출처 확인 상태에 따라 게시 여부를 관리합니다.
        </p>
      </section>
      <AdminBenefitsPanel />
    </div>
  );
}
