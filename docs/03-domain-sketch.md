# 03. 도메인 스케치

## 1. 도메인 핵심

이 프로젝트의 핵심 도메인은 “혜택”이 아니라 **검증 가능한 혜택 정보**다.

따라서 단순히 Brand와 Benefit만 있으면 부족하다.

반드시 아래 개념이 포함되어야 한다.

- 브랜드
- 혜택
- 혜택 출처
- 검수 상태
- 태그/조건
- 사용자 제보
- 검수 로그
- 조회수

## 2. 핵심 엔티티

### Category

브랜드의 큰 분류.

예시:

- 카페
- 베이커리
- 외식
- 영화·문화
- 뷰티
- 테마파크

주요 필드:

- id
- name
- slug
- displayOrder
- isActive
- createdAt
- updatedAt

### Brand

혜택을 제공하는 브랜드.

주요 필드:

- id
- categoryId
- name
- slug
- description
- officialUrl
- membershipUrl
- appUrl
- brandColor
- logoUrl
- isActive
- createdAt
- updatedAt

주의:

MVP에서는 로고 사용을 최소화한다. logoUrl은 nullable로 둔다.

### Benefit

브랜드가 제공하는 혜택.

주요 필드:

- id
- brandId
- title
- summary
- detail
- benefitType
- occasionType
- birthdayTimingType
- conditionSummary
- requiredApp
- requiredMembership
- requiredPurchase
- membershipGrade
- usagePeriodDescription
- availableFrom
- availableTo
- caution
- verificationStatus
- lastVerifiedAt
- isActive
- createdAt
- updatedAt

### BenefitSource

혜택의 공식 출처.

주요 필드:

- id
- benefitId
- sourceType
- sourceUrl
- sourceTitle
- sourceCheckedAt
- contentHash
- memo
- createdAt
- updatedAt

BenefitSource를 분리하는 이유:

- 하나의 혜택이 여러 출처를 가질 수 있다.
- 공식 출처와 참고 출처를 구분해야 한다.
- 추후 공식 페이지 변경 감지에 사용할 수 있다.
- 정보 신뢰도 관리의 핵심이다.

### Tag

조건형 필터.

예시:

- 무료
- 할인
- 쿠폰
- 앱 필요
- 앱 불필요
- 회원가입 필요
- 멤버십 필요
- 조건 없음
- 최소 구매 필요
- 생일 당일
- 생일월
- 생일 전후 7일
- 공식 확인
- 확인 필요

### BenefitTag

Benefit과 Tag의 다대다 연결.

중복 방지를 위해 benefitId + tagId unique constraint를 둔다.

### UserReport

사용자 정보 수정 제보.

주요 필드:

- id
- brandId
- benefitId
- reportType
- content
- referenceUrl
- email
- status
- createdAt
- resolvedAt

### VerificationLog

혜택 검수 상태 변경 로그.

주요 필드:

- id
- benefitId
- adminUserId
- beforeStatus
- afterStatus
- memo
- verifiedAt

### PageViewDaily

조회수 집계.

주요 필드:

- id
- targetType
- targetId
- viewDate
- viewCount
- createdAt
- updatedAt

targetType + targetId + viewDate unique constraint를 둔다.

### AdminUser

관리자 계정.

MVP에서는 관리자 1명만 있어도 된다.

## 3. Enum

### BenefitType

- FREE_ITEM
- DISCOUNT
- COUPON
- POINT
- GIFT
- UPGRADE
- ETC

### OccasionType

- BIRTHDAY
- NEW_SIGNUP
- APP_INSTALL
- FIRST_PURCHASE
- ANNIVERSARY
- SEASON

MVP에서는 BIRTHDAY만 사용한다.

### BirthdayTimingType

- BIRTHDAY_ONLY
- BIRTHDAY_MONTH
- BEFORE_AFTER_DAYS
- ISSUED_BEFORE_BIRTHDAY
- UNKNOWN

### VerificationStatus

- DRAFT
- NEEDS_CHECK
- VERIFIED
- PUBLISHED
- EXPIRED
- HIDDEN

권장 흐름:

DRAFT → NEEDS_CHECK → VERIFIED → PUBLISHED

변경 감지 또는 사용자 제보 발생 시:

PUBLISHED → NEEDS_CHECK

혜택 종료 확인 시:

PUBLISHED → EXPIRED

### SourceType

- OFFICIAL_HOME
- OFFICIAL_APP
- OFFICIAL_MEMBERSHIP
- OFFICIAL_FAQ
- OFFICIAL_NOTICE
- OFFICIAL_SNS
- CUSTOMER_CENTER
- BLOG_REFERENCE
- COMMUNITY_REFERENCE

### ReportType

- WRONG_INFO
- BENEFIT_ENDED
- CONDITION_CHANGED
- NEW_BENEFIT
- OFFICIAL_LINK_FOUND
- ETC

### ReportStatus

- RECEIVED
- REVIEWING
- RESOLVED
- REJECTED

### ViewTargetType

- BRAND
- BENEFIT
- CATEGORY
- TAG

### AdminRole

- SUPER_ADMIN
- EDITOR

## 4. 확장성

생일 혜택 전용 서비스로 시작하지만, 내부 구조는 Benefit 중심으로 둔다.

추후 아래로 확장 가능하다.

- 신규 가입 혜택
- 앱 설치 혜택
- 첫 구매 혜택
- 기념일 혜택
- 시즌 혜택

이때 occasionType만 확장하면 된다.

단, 화면에서는 MVP 기간 동안 생일 혜택만 노출한다.
