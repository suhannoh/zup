import type { Metadata } from "next";
import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import "./globals.css";

export const metadata: Metadata = {
  title: "Zup - 몰라서 못 받던 혜택, 오늘 줍자",
  description:
    "Zup은 브랜드별 생일 쿠폰과 무료 혜택을 공식 출처 기준으로 정리하는 정보 큐레이션 서비스입니다. 앱 필요 여부, 멤버십 조건, 사용 기간을 한눈에 확인하세요.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <Header />
        <main className="mx-auto min-h-[calc(100vh-240px)] max-w-6xl px-5 py-10 md:px-12">
          {children}
        </main>
        <Footer />
      </body>
    </html>
  );
}
