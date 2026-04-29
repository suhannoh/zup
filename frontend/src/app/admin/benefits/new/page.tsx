import { AdminAuthGate } from "@/components/admin/AdminAuthGate";
import { AdminManualBenefitCreatePanel } from "@/components/admin/AdminManualBenefitCreatePanel";

export default function AdminBenefitNewPage() {
  return (
    <AdminAuthGate>
      <AdminManualBenefitCreatePanel />
    </AdminAuthGate>
  );
}
