import { AdminBenefitDetailPanel } from "@/components/admin/AdminBenefitDetailPanel";

export default async function AdminBenefitDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-3xl font-bold">혜택 운영 상세</h1>
        <p className="max-w-3xl text-sm leading-6 text-neutral-600">
          혜택의 공식 출처, 조건 태그, 검수 이력을 관리합니다.
        </p>
      </section>
      <AdminBenefitDetailPanel benefitId={Number(id)} />
    </div>
  );
}
