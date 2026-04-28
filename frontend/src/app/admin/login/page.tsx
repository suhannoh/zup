import { Suspense } from "react";
import { AdminLoginForm } from "@/components/admin/AdminLoginForm";

export default function AdminLoginPage() {
  return (
    <Suspense fallback={<div className="text-sm text-neutral-500">로그인 화면을 불러오는 중입니다.</div>}>
      <AdminLoginForm />
    </Suspense>
  );
}
