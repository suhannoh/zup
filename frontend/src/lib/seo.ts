export const SITE_URL = (
  process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"
).replace(/\/$/, "");

export const DEFAULT_TITLE = "Zup - 몰라서 못 받던 혜택, 오늘 줍자";

export const DEFAULT_DESCRIPTION =
  "Zup은 브랜드별 생일 쿠폰과 무료 혜택을 공식 출처 기준으로 정리하는 정보 큐레이션 서비스입니다. 앱 필요 여부, 멤버십 조건, 사용 기간을 한눈에 확인하세요.";

export const DEFAULT_KEYWORDS = [
  "생일 혜택",
  "생일 쿠폰",
  "브랜드 생일 혜택",
  "무료 생일 쿠폰",
  "카페 생일 혜택",
  "영화관 생일 혜택",
  "Zup",
  "줍",
];

export function absoluteUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${SITE_URL}${normalizedPath}`;
}
