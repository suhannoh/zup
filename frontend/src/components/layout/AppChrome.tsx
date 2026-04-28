"use client";

import { usePathname } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export function AppChrome({ children }: { children: React.ReactNode }) {
    const pathname = usePathname();
    const isAdmin = pathname.startsWith("/admin");

    if (isAdmin) {
        return <>{children}</>;
    }

    return (
        <>
            <Header />
            <main className="mx-auto min-h-[calc(100vh-240px)] max-w-6xl px-5 py-10 md:px-12">
                {children}
            </main>
            <Footer />
        </>
    );
}