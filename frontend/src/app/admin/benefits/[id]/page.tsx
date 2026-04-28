import { AdminBenefitDetailPanel } from "@/components/admin/AdminBenefitDetailPanel";

export default async function AdminBenefitDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <AdminBenefitDetailPanel benefitId={Number(id)} />;
}
