"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { hasAdminAuth } from "@/lib/adminAuth";

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
    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm font-semibold text-neutral-500">
          관리자 인증 상태를 확인하는 중입니다.
        </div>
    );
  }

  return <>{children}</>;
}