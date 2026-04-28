import { AdminBenefitCandidateDetailPanel } from "@/components/admin/AdminBenefitCandidateDetailPanel";

export default async function AdminBenefitCandidateDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <AdminBenefitCandidateDetailPanel candidateId={Number(id)} />;
}
