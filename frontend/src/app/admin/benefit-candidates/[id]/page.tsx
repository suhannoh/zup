import { AdminBenefitCandidateDetailPanel } from "@/components/admin/AdminBenefitCandidateDetailPanel";

export default async function AdminBenefitCandidateDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">혜택 후보 상세</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          근거 문장과 추정 필드를 확인한 뒤, 필요한 값을 수정해 Benefit으로 승인합니다.
        </p>
      </section>
      <AdminBenefitCandidateDetailPanel candidateId={Number(id)} />
    </div>
  );
}
