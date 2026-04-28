"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { hasAdminAuth } from "@/lib/adminAuth";
import { AdminLogoutButton } from "@/components/admin/AdminLogoutButton";

export function AdminAuthGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [checked, setChecked] = useState(false);
  const isLoginPage = pathname === "/admin/login";

  useEffect(() => {
    if (isLoginPage) {
      setChecked(true);
      return;
    }

    if (!hasAdminAuth()) {
      router.replace(`/admin/login?next=${encodeURIComponent(pathname)}`);
      return;
    }

    setChecked(true);
  }, [isLoginPage, pathname, router]);

  if (!checked) {
    return <div className="text-sm text-neutral-500">관리자 인증 상태를 확인하는 중입니다.</div>;
  }

  if (isLoginPage) {
    return children;
  }

  return (
    <div className="space-y-5">
      <div className="flex justify-end">
        <AdminLogoutButton />
      </div>
      {children}
    </div>
  );
}
