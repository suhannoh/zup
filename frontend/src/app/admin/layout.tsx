import type { Metadata } from "next";
import { AdminAuthGate } from "@/components/admin/AdminAuthGate";

export const metadata: Metadata = {
  robots: {
    index: false,
    follow: false,
  },
};

export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <AdminAuthGate>{children}</AdminAuthGate>;
}
