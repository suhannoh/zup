import type { SourceType } from "@/types/adminBenefitSource";

export const SOURCE_TYPE_LABELS: Record<SourceType, string> = {
  OFFICIAL_HOME: "공식 홈페이지",
  OFFICIAL_APP: "공식 앱",
  OFFICIAL_MEMBERSHIP: "공식 멤버십",
  OFFICIAL_FAQ: "공식 FAQ",
  OFFICIAL_NOTICE: "공식 공지",
  OFFICIAL_SNS: "공식 SNS",
  CUSTOMER_CENTER: "고객센터",
  BLOG_REFERENCE: "블로그 참고",
  COMMUNITY_REFERENCE: "커뮤니티 참고",
};

export const SOURCE_TYPE_OPTIONS: SourceType[] = [
  "OFFICIAL_HOME",
  "OFFICIAL_APP",
  "OFFICIAL_MEMBERSHIP",
  "OFFICIAL_FAQ",
  "OFFICIAL_NOTICE",
  "OFFICIAL_SNS",
  "CUSTOMER_CENTER",
  "BLOG_REFERENCE",
  "COMMUNITY_REFERENCE",
];
