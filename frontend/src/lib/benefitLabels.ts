import type {
  BenefitType,
  BirthdayTimingType,
  OccasionType,
  VerificationStatus,
} from "@/types/adminBenefit";

export const BENEFIT_TYPE_LABELS: Record<BenefitType, string> = {
  FREE_ITEM: "무료 제공",
  DISCOUNT: "할인",
  COUPON: "쿠폰",
  POINT: "포인트",
  GIFT: "선물",
  UPGRADE: "업그레이드",
  ETC: "기타",
};

export const BIRTHDAY_TIMING_LABELS: Record<BirthdayTimingType, string> = {
  BIRTHDAY_ONLY: "생일 당일",
  BIRTHDAY_MONTH: "생일월",
  BEFORE_AFTER_DAYS: "생일 전후",
  ISSUED_BEFORE_BIRTHDAY: "생일 전 발급",
  UNKNOWN: "확인 필요",
};

export const VERIFICATION_STATUS_LABELS: Record<VerificationStatus, string> = {
  DRAFT: "작성 중",
  NEEDS_CHECK: "검수 필요",
  VERIFIED: "검수 완료",
  PUBLISHED: "게시 중",
  EXPIRED: "종료됨",
  HIDDEN: "숨김",
};

export const OCCASION_TYPE_LABELS: Record<OccasionType, string> = {
  BIRTHDAY: "생일",
  NEW_SIGNUP: "신규 가입",
  APP_INSTALL: "앱 설치",
  FIRST_PURCHASE: "첫 구매",
  ANNIVERSARY: "기념일",
  SEASON: "시즌",
};

export const BENEFIT_TYPE_OPTIONS: BenefitType[] = [
  "FREE_ITEM",
  "DISCOUNT",
  "COUPON",
  "POINT",
  "GIFT",
  "UPGRADE",
  "ETC",
];

export const BIRTHDAY_TIMING_OPTIONS: BirthdayTimingType[] = [
  "BIRTHDAY_ONLY",
  "BIRTHDAY_MONTH",
  "BEFORE_AFTER_DAYS",
  "ISSUED_BEFORE_BIRTHDAY",
  "UNKNOWN",
];

export const VERIFICATION_STATUS_OPTIONS: VerificationStatus[] = [
  "DRAFT",
  "NEEDS_CHECK",
  "VERIFIED",
  "PUBLISHED",
  "EXPIRED",
  "HIDDEN",
];

export const OCCASION_TYPE_OPTIONS: OccasionType[] = [
  "BIRTHDAY",
  "NEW_SIGNUP",
  "APP_INSTALL",
  "FIRST_PURCHASE",
  "ANNIVERSARY",
  "SEASON",
];
