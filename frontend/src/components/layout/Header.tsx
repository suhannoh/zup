"use client";

import Link from "next/link";
import { useUiStore } from "@/stores/useUiStore";

const links = [
  { href: "/brands", label: "브랜드별 혜택" },
  { href: "/categories/cafe", label: "카테고리" },
  { href: "/reports/new", label: "정보 제보하기" },
  { href: "/admin", label: "관리자" },
];

export function Header() {
  const { isMobileMenuOpen, openMobileMenu, closeMobileMenu } = useUiStore();

  return (
    <header className="border-b border-border bg-white">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5 md:px-12">
        <Link href="/" className="font-bold" onClick={closeMobileMenu}>
          <span className="block text-xl">Zup</span>
          <span className="block text-xs font-normal text-neutral-400">몰라서 못 받던 혜택, 오늘 줍자</span>
        </Link>
        <nav className="hidden items-center gap-6 text-sm font-medium text-neutral-600 md:flex">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className="hover:text-accent">
              {link.label}
            </Link>
          ))}
        </nav>
        <button
          type="button"
          className="rounded-md border border-border px-3 py-2 text-sm md:hidden"
          onClick={isMobileMenuOpen ? closeMobileMenu : openMobileMenu}
          aria-expanded={isMobileMenuOpen}
        >
          메뉴
        </button>
      </div>
      {isMobileMenuOpen ? (
        <nav className="border-t border-border bg-white px-5 py-4 md:hidden">
          <div className="flex flex-col gap-3 text-sm font-medium text-neutral-700">
            {links.map((link) => (
              <Link key={link.href} href={link.href} onClick={closeMobileMenu}>
                {link.label}
              </Link>
            ))}
          </div>
        </nav>
      ) : null}
    </header>
  );
}
