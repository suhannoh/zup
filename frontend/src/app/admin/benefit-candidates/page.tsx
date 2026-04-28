import { AdminBenefitCandidatesPanel } from "@/components/admin/AdminBenefitCandidatesPanel";

export default function AdminBenefitCandidatesPage() {
  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">혜택 후보 검수</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          자동 수집된 후보의 근거 문장을 확인하고, 승인 대상은 Benefit으로 전환합니다.
        </p>
      </section>
      <AdminBenefitCandidatesPanel />
    </div>
  );
}
