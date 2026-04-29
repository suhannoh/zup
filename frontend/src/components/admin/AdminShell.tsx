"use client";

import Link from "next/link";
import {usePathname} from "next/navigation";
import {useEffect, useState} from "react";
import {AdminAuthGate} from "@/components/admin/AdminAuthGate";
import {AdminLogoutButton} from "@/components/admin/AdminLogoutButton";

type IconProps = {
    className?: string;
};

function DashboardIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 13h7V4H4v9Zm0 7h7v-5H4v5Zm9 0h7v-9h-7v9Zm0-16v5h7V4h-7Z" stroke="currentColor" strokeWidth="1.8"
                  strokeLinejoin="round"/>
        </svg>
    );
}

function BrandIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M5 10h14l-1-5H6l-1 5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <path d="M6 10v9h12v-9" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <path d="M9 19v-5h6v5" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
        </svg>
    );
}

function SourceIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z" stroke="currentColor" strokeWidth="1.8"/>
            <path d="M3.5 12h17" stroke="currentColor" strokeWidth="1.8"/>
            <path d="M12 3c2.2 2.4 3.3 5.4 3.3 9S14.2 18.6 12 21c-2.2-2.4-3.3-5.4-3.3-9S9.8 5.4 12 3Z"
                  stroke="currentColor" strokeWidth="1.8"/>
        </svg>
    );
}

function CandidateIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M7 4h10v16H7V4Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <path d="M9.5 8h5M9.5 12h5M9.5 16h3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
            <path d="m15 17 1.2 1.2L19 15.4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"
                  strokeLinejoin="round"/>
        </svg>
    );
}

function BenefitIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M20 8h-3.2A3 3 0 1 0 12 4.5 3 3 0 1 0 7.2 8H4v12h16V8Z" stroke="currentColor" strokeWidth="1.8"
                  strokeLinejoin="round"/>
            <path d="M12 8v12M4 12h16" stroke="currentColor" strokeWidth="1.8"/>
        </svg>
    );
}

function HistoryIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 12a8 8 0 1 0 2.35-5.65L4 8.7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"
                  strokeLinejoin="round"/>
            <path d="M4 4v4.7h4.7M12 7.5V12l3 2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"
                  strokeLinejoin="round"/>
        </svg>
    );
}

function ReportIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M5 4h14v13H8l-3 3V4Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <path d="M12 7.5v4M12 14.5h.01" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"/>
        </svg>
    );
}

function PanelCloseIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 5h16v14H4V5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <path d="M9 5v14M15 9l-3 3 3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"
                  strokeLinejoin="round"/>
        </svg>
    );
}

function PanelOpenIcon({className}: IconProps) {
    return (
        <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 5h16v14H4V5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <path d="M9 5v14M12 9l3 3-3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"
                  strokeLinejoin="round"/>
        </svg>
    );
}

const navItems = [
    {href: "/admin", label: "대시보드", icon: DashboardIcon},
    {href: "/admin/brands", label: "브랜드 관리", icon: BrandIcon},
    {href: "/admin/source-watches", label: "공식 출처 수집", icon: SourceIcon},
    {href: "/admin/benefit-candidates", label: "새 혜택 검수", icon: CandidateIcon},
    {href: "/admin/benefits", label: "공개 혜택 관리", icon: BenefitIcon},
    {href: "/admin/collection-runs", label: "수집 이력", icon: HistoryIcon},
    {href: "/admin/reports", label: "제보 관리", icon: ReportIcon},
];

function isActivePath(pathname: string, href: string) {
    if (href === "/admin") return pathname === "/admin";
    return pathname === href || pathname.startsWith(`${href}/`);
}

function getCurrentMenuLabel(pathname: string) {
    return navItems.find((item) => isActivePath(pathname, item.href))?.label ?? "관리자";
}

export function AdminShell({children}: { children: React.ReactNode }) {
    const pathname = usePathname();
    const [collapsed, setCollapsed] = useState(false);
    const [hovered, setHovered] = useState(false);
    const sidebarExpanded = !collapsed || hovered;
    const isLoginPage = pathname === "/admin/login";
    const currentMenuLabel = getCurrentMenuLabel(pathname);

    useEffect(() => {
        const saved = window.localStorage.getItem("zup-admin-sidebar-collapsed");
        if (saved === "true") setCollapsed(true);
    }, []);

    function toggleSidebar() {
        setCollapsed((prev) => {
            const next = !prev;
            window.localStorage.setItem("zup-admin-sidebar-collapsed", String(next));
            return next;
        });
    }

    if (isLoginPage) {
        return <AdminAuthGate>{children}</AdminAuthGate>;
    }

    return (
        <AdminAuthGate>
            <div className="min-h-screen bg-slate-50 text-neutral-950">
                <aside
                    onMouseEnter={() => {
                        if (collapsed) setHovered(true);
                    }}
                    onMouseLeave={() => {
                        if (collapsed) setHovered(false);
                    }}
                    className={[
                        "fixed left-0 top-0 z-30 h-screen border-r border-neutral-200 bg-white shadow-sm transition-all duration-200",
                        sidebarExpanded ? "w-60" : "w-[108px]",
                    ].join(" ")}
                >
                    <div
                        className={[
                            "border-b border-neutral-200",
                            sidebarExpanded
                                ? "flex h-20 items-center gap-2 px-4"
                                : "flex h-24 flex-col items-center justify-center px-2",
                        ].join(" ")}
                    >
                        <Link
                            href="/"
                            className={[
                                "group flex min-w-0 items-center rounded-xl transition hover:bg-neutral-50",
                                sidebarExpanded ? "gap-3 px-2 py-2" : "justify-center p-1"
                            ].join(" ")}
                            title="Zup 홈으로 이동"
                        >
                            <div
                                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-blue-600 text-base font-black text-white">
                                Z
                            </div>

                            {sidebarExpanded ? (
                                <div className="min-w-0">
                                    <p className="text-lg font-black leading-none tracking-tight text-neutral-950">Zup</p>
                                    <p className="mt-1 truncate text-xs font-medium text-neutral-500">
                                        몰라서 못 받던 혜택, 오늘 줍자
                                    </p>
                                </div>
                            ) : null}
                        </Link>

                        <button
                            type="button"
                            onClick={toggleSidebar}
                            className={[
                                "flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-neutral-200 bg-white text-neutral-500 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600",
                                sidebarExpanded ? "ml-auto" : "mx-auto mt-2",
                            ].join(" ")}
                            aria-label={collapsed ? "사이드바 펼치기" : "사이드바 접기"}
                            title={collapsed ? "사이드바 펼치기" : "사이드바 접기"}
                        >
                            {collapsed ? <PanelOpenIcon className="h-6 w-6"/> : <PanelCloseIcon className="h-6 w-6"/>}
                        </button>
                    </div>

                    <nav className={sidebarExpanded ? "space-y-1 px-3 py-4" : "space-y-2 px-5 py-5"}>
                        {navItems.map((item) => {
                            const active = isActivePath(pathname, item.href);
                            const Icon = item.icon;

                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    className={[
                                        "group flex items-center rounded-xl text-sm font-semibold transition",
                                        sidebarExpanded ? "h-11 gap-3 px-4" : "h-12 justify-center px-0",
                                        active ? "bg-blue-600 text-white" : "text-neutral-700 hover:bg-neutral-100",
                                    ].join(" ")}
                                    title={item.label}
                                >
                                  <span
                                      className={[
                                          "flex shrink-0 items-center justify-center",
                                          sidebarExpanded ? "h-6 w-6" : "h-8 w-8",
                                      ].join(" ")}
                                  >
                                    <Icon
                                        className={[
                                            "shrink-0",
                                            sidebarExpanded ? "h-[22px] w-[22px]" : "h-[26px] w-[26px]",
                                        ].join(" ")}
                                    />
                                  </span>

                                    {sidebarExpanded ? <span className="truncate">{item.label}</span> : null}
                                </Link>
                            );
                        })}
                    </nav>
                </aside>

                <div
                    className={[
                        "min-h-screen transition-all duration-200",
                        collapsed ? "pl-[108px]" : "pl-60",
                    ].join(" ")}
                >
                    <header className="sticky top-0 z-20 border-b border-neutral-200 bg-white/95 backdrop-blur">
                        <div className="flex h-16 w-full items-center justify-between gap-4 px-6 lg:px-8">
                            <p className="text-sm font-bold text-neutral-950">{currentMenuLabel}</p>

                            <div className="flex items-center gap-3">
                                <Link
                                    href="/"
                                    className="rounded-lg border border-neutral-200 bg-white px-3 py-2 text-xs font-semibold text-neutral-700 hover:bg-neutral-50"
                                >
                                    사용자 화면
                                </Link>
                                <AdminLogoutButton/>
                            </div>
                        </div>
                    </header>

                    <main className="w-full px-8 py-8">
                        {children}
                    </main>
                </div>
            </div>
        </AdminAuthGate>
    );
}
