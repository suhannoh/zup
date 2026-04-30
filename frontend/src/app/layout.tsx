import type { Metadata } from "next";
import { AppChrome } from "@/components/layout/AppChrome";
import { DEFAULT_DESCRIPTION, DEFAULT_KEYWORDS, DEFAULT_TITLE, SITE_URL } from "@/lib/seo";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: DEFAULT_TITLE,
  description: DEFAULT_DESCRIPTION,
  keywords: DEFAULT_KEYWORDS,
  alternates: {
    canonical: "/",
  },
  openGraph: {
    title: DEFAULT_TITLE,
    description: DEFAULT_DESCRIPTION,
    type: "website",
    siteName: "Zup",
    locale: "ko_KR",
    url: "/",
  },
  twitter: {
    card: "summary_large_image",
    title: DEFAULT_TITLE,
    description: DEFAULT_DESCRIPTION,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
      <AppChrome>
          {children}
      </AppChrome>
      </body>
    </html>
  );
}
