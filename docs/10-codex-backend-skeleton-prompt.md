# 10. Codex 작업 지시서 02 — 백엔드 도메인 스켈레톤 생성

## 목표

`docs/03-domain-sketch.md`, `docs/04-api-spec.md`, `docs/05-backend-architecture.md`를 읽고 백엔드 핵심 도메인 스켈레톤을 생성한다.

이번 작업은 완성된 비즈니스 로직이 아니라, 다음 단계에서 실제 CRUD와 조회 API를 구현할 수 있도록 기반을 잡는 것이다.

## 작업 범위

- Entity 생성
- Enum 생성
- Repository 생성
- 기본 DTO 생성
- Public Controller 골격
- Admin Controller 골격
- Service 골격
- Seed 데이터 준비
- 빌드 성공

## 생성할 엔티티

- Category
- Brand
- Benefit
- BenefitSource
- Tag
- BenefitTag
- UserReport
- VerificationLog
- PageViewDaily
- AdminUser

자세한 필드는 `docs/03-domain-sketch.md`를 따른다.

## Enum

다음 enum을 생성한다.

- BenefitType
- OccasionType
- BirthdayTimingType
- VerificationStatus
- SourceType
- ReportType
- ReportStatus
- ViewTargetType
- AdminRole

## Repository

각 엔티티별 JpaRepository를 생성한다.

필요한 기본 메서드:

- BrandRepository.findBySlug
- CategoryRepository.findBySlug
- TagRepository.findBySlug
- BenefitRepository.findByBrandId
- UserReportRepository.findByStatus
- PageViewDailyRepository.findByTargetTypeAndTargetIdAndViewDate

## API 골격

### Public API

```text
GET /api/v1/categories
GET /api/v1/tags
GET /api/v1/brands
GET /api/v1/brands/{slug}
GET /api/v1/categories/{slug}/benefits
GET /api/v1/tags/{slug}/benefits
POST /api/v1/reports
POST /api/v1/page-views
```

### Admin API

```text
GET /api/v1/admin/brands
POST /api/v1/admin/brands
PATCH /api/v1/admin/brands/{id}

GET /api/v1/admin/benefits
POST /api/v1/admin/benefits
PATCH /api/v1/admin/benefits/{id}
PATCH /api/v1/admin/benefits/{id}/status

GET /api/v1/admin/reports
PATCH /api/v1/admin/reports/{id}/status

GET /api/v1/admin/review-needed
GET /api/v1/admin/stale-benefits
GET /api/v1/admin/dashboard
```

이번 작업에서는 임시 응답 또는 최소 로직으로 구현해도 된다.

단, 빌드는 성공해야 한다.

## Seed 데이터

Category seed:

- 카페 / cafe
- 베이커리 / bakery
- 외식 / restaurant
- 영화·문화 / movie-culture
- 뷰티 / beauty
- 테마파크 / theme-park

Tag seed:

- 무료 / free
- 할인 / discount
- 쿠폰 / coupon
- 앱 필요 / app-required
- 앱 불필요 / no-app-required
- 회원가입 필요 / signup-required
- 멤버십 필요 / membership-required
- 조건 없음 / no-condition
- 최소 구매 필요 / purchase-required
- 생일 당일 / birthday-only
- 생일월 / birthday-month
- 생일 전후 7일 / birthday-week
- 공식 확인 / officially-verified
- 확인 필요 / needs-check

## 주의사항

- 자동 크롤링 구현 금지
- AI 추천 구현 금지
- 사용자 회원가입 구현 금지
- 브랜드 로고 수집 금지
- 관리자 인증은 다음 작업으로 미룸
- 엔티티 설계는 과도하게 복잡하게 만들지 말 것

## 완료 보고

작업 완료 후 아래를 보고한다.

1. 생성/수정 파일
2. 엔티티 목록
3. enum 목록
4. API 목록
5. seed 데이터 방식
6. 빌드 결과
7. 다음 TODO
