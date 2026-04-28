"use client";

import { useRouter } from "next/navigation";
import { clearAdminAuth } from "@/lib/adminAuth";

export function AdminLogoutButton() {
  const router = useRouter();

  function handleLogout() {
    clearAdminAuth();
    router.replace("/admin/login");
  }

  return (
    <button
      className="rounded-lg border border-border bg-white px-3 py-2 text-sm font-semibold hover:border-accent"
      type="button"
      onClick={handleLogout}
    >
      로그아웃
    </button>
  );
}
