import { AdminBrandsPanel } from "@/components/admin/AdminBrandsPanel";

export default function AdminBrandsPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">브랜드 관리</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          Zup에 노출할 브랜드 정보를 등록하고, 카테고리와 공식 링크를 관리합니다.
        </p>
      </section>
      <AdminBrandsPanel />
    </div>
  );
}
