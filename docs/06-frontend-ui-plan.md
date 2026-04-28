# 06. 프론트엔드 UI 계획

## 1. 프론트엔드 목표

프론트엔드는 "앱"보다 **검색 유입형 정보 사이트**처럼 보여야 한다.

핵심 목표:

- 사용자가 로그인 없이 바로 혜택을 확인한다.
- 검색 결과에서 들어온 사용자가 원하는 정보를 빠르게 찾는다.
- 브랜드 상세 페이지가 SEO에 유리한 구조를 가진다.
- 모바일에서 읽기 쉽다.
- 필터와 배지로 블로그보다 빠르게 비교한다.

---

## 2. 기술 스택

- Next.js
- React
- TypeScript
- Tailwind CSS
- Axios
- Zustand

SEO가 중요하므로 React + Vite가 아니라 Next.js를 사용한다.

---

## 3. 라우트 구조

```text
/
 /brands
 /brands/[slug]
 /categories/[slug]
 /tags/[slug]
 /reports/new
 /admin
 /admin/brands
 /admin/benefits
 /admin/reports
```

---

## 4. 디자인 시스템

### 4-1. 컬러 토큰

```css
/* ─── Base ─── */
--color-bg:          #F8F8F6;   /* 전체 배경: 순백보다 한 톤 낮은 따뜻한 오프화이트 */
--color-surface:     #FFFFFF;   /* 카드, 패널 배경 */
--color-border:      #EAEAE6;   /* 구분선, 카드 테두리 */
--color-border-muted:#F2F2EF;   /* 섹션 내부 얇은 구분 */

/* ─── Text ─── */
--color-text-primary:   #1A1A1A;  /* 제목, 핵심 텍스트 */
--color-text-secondary: #6B6B6B;  /* 부제목, 레이블 */
--color-text-muted:     #A3A3A3;  /* 날짜, 메타 정보 */
--color-text-inverse:   #FFFFFF;  /* 다크 배경 위 텍스트 */

/* ─── Brand Accent (포인트 컬러 1개) ─── */
--color-accent:         #3B6FE8;  /* 버튼, 링크, 활성 배지 — 신뢰감 있는 블루 */
--color-accent-light:   #EEF3FD;  /* 배지 배경, hover 상태 */
--color-accent-dark:    #2A55C7;  /* 버튼 hover, pressed */

/* ─── Status ─── */
--color-verified:    #16A34A;   /* 공식 확인 배지 */
--color-verified-bg: #F0FDF4;
--color-warning:     #D97706;   /* 재확인 필요 */
--color-warning-bg:  #FFFBEB;
--color-expired:     #DC2626;   /* 혜택 종료 */
--color-expired-bg:  #FEF2F2;
--color-muted-tag:   #F4F4F2;   /* 일반 조건 배지 배경 */
--color-muted-tag-text: #4A4A4A;
```

> **원칙:** 브랜드 포인트 컬러는 `--color-accent` 하나만 사용한다. 브랜드별 컬러를 무분별하게 사용하면 신뢰 기반 정보 서비스가 아니라 광고 사이트처럼 보인다.

---

### 4-2. 타이포그래피

```css
/* ─── Font Stack ─── */
font-family: 'Pretendard Variable', 'Pretendard', -apple-system, BlinkMacSystemFont,
'Apple SD Gothic Neo', sans-serif;

/* ─── Scale ─── */
--text-xs:   12px / 1.5  /* 날짜, 출처 레이블 */
--text-sm:   14px / 1.6  /* 배지, 보조 텍스트 */
--text-base: 16px / 1.7  /* 본문 */
--text-lg:   18px / 1.6  /* 카드 제목 */
--text-xl:   22px / 1.4  /* 섹션 헤딩 */
--text-2xl:  28px / 1.3  /* 페이지 제목 */
--text-3xl:  38px / 1.2  /* 히어로 카피 (모바일: 26px) */

/* ─── Weight ─── */
Regular  400  /* 본문 */
Medium   500  /* 레이블, 배지 */
Semibold 600  /* 카드 제목, 강조 */
Bold     700  /* 히어로, 페이지 제목 */
```

---

### 4-3. 스페이싱 & 그리드

```text
컨텐츠 최대 너비:  1120px
패딩 (데스크탑):   0 48px
패딩 (모바일):     0 20px

섹션 간격:         80px (모바일: 56px)
카드 내부 패딩:    24px (모바일: 20px)
카드 간 갭:        16px (모바일: 12px)

브랜드 카드 그리드:
  데스크탑:  4열 (280px 기준)
  태블릿:    3열
  모바일:    2열 또는 1열 (혜택 상세는 1열 강제)
```

---

### 4-4. 반경 & 그림자

```css
/* ─── Border Radius ─── */
--radius-sm:   6px    /* 배지, 소형 태그 */
                --radius-md:   12px   /* 카드 */
                                 --radius-lg:   16px   /* 모달, 드로어 */
                                                  --radius-pill: 999px  /* 필터 버튼, 조건 칩 */

                                                                        /* ─── Shadow ─── */
                                                                    --shadow-card: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
--shadow-hover: 0 4px 16px rgba(0,0,0,0.09);
--shadow-modal: 0 20px 60px rgba(0,0,0,0.14);
```

> **원칙:** 그림자는 얕게. 입체감을 강조하면 광고 느낌이 난다. 카드는 테두리(`--color-border`) + 얕은 그림자 조합으로 충분하다.

---

### 4-5. 공통 컴포넌트 스펙

#### 조건 배지 (Condition Badge)

```text
크기:      height 26px, padding 0 10px
폰트:      12px Medium
반경:      999px (pill)
간격:      4px gap (배지 간)

유형별 컬러:
  무료          bg #EEF3FD  text #3B6FE8  (accent)
  할인          bg #F0FDF4  text #16A34A
  앱 필요       bg #F4F4F2  text #4A4A4A
  앱 불필요     bg #F0FDF4  text #16A34A
  멤버십 필요   bg #FFF7ED  text #C2410C
  조건 없음     bg #F0FDF4  text #16A34A
  생일 당일     bg #EEF3FD  text #3B6FE8
  생일월        bg #F5F3FF  text #6D28D9
```

#### 공식 확인 배지 (Verified Badge)

```text
크기:        height 24px, padding 0 8px
아이콘:      체크 아이콘 14px + 텍스트 "공식 확인"
폰트:        12px Medium
배경:        #F0FDF4
텍스트:      #16A34A
반경:        999px
```

#### 최근 확인일 레이블

```text
폰트:    12px Regular
컬러:    --color-text-muted (#A3A3A3)
형식:    "2026.04.28 확인"
위치:    카드 하단 또는 상세 페이지 상단 인포바
```

#### 버튼

```css
/* Primary */
height: 48px;
padding: 0 24px;
border-radius: 10px;
background: var(--color-accent);
color: white;
font-size: 15px;
font-weight: 600;
transition: background 150ms;

/* hover */
background: var(--color-accent-dark);

/* Secondary */
background: transparent;
border: 1.5px solid var(--color-border);
color: var(--color-text-primary);

/* Ghost (텍스트 버튼) */
background: transparent;
color: var(--color-accent);
padding: 0 8px;
```

---

## 5. 기본 레이아웃

### Header

```text
높이:         64px (고정)
배경:         #FFFFFF + border-bottom 1px solid #EAEAE6
레이아웃:     logo 좌측 + nav 중앙 + (없음) 우측

로고:
  텍스트:     "줍"
  폰트:       28px Bold
  컬러:       #1A1A1A
  옆 레이블:  "생일 혜택 모음" — 12px Regular, #A3A3A3

Nav 링크:     14px Medium, #6B6B6B
              hover: #1A1A1A, underline 없음
              active: #3B6FE8

모바일:
  height: 56px
  로고만 표시 + 햄버거 아이콘 우측
  드로어: 우측에서 슬라이드, 전체 메뉴 표시
```

### Footer

```text
배경:         #1A1A1A  (다크)
텍스트:       #A3A3A3
패딩:         60px 0

구성:
  상단:  서비스명 + 한 줄 설명
  중단:  면책 문구 (14px, #6B6B6B)
  하단:  copyright

면책 문구 스타일:
  max-width: 640px
  line-height: 1.8
  border-left: 2px solid #3B3B3B
  padding-left: 16px
  font-size: 13px
```

---

## 6. 메인 페이지

### 히어로 섹션

```text
배경:         #F8F8F6 (배경과 동일, 구분 없이 자연스럽게)
패딩:         120px 0 80px

레이아웃:     중앙 정렬, 텍스트만, 이미지/일러스트 없음
              (이미지를 넣으면 정보 사이트보다 광고처럼 보임)

타이틀:
  "생일에 받을 수 있는
   브랜드 혜택, 조건별로 한 번에"
  38px Bold, #1A1A1A
  모바일: 26px

서브타이틀:
  "무료 쿠폰, 앱 필요 여부, 멤버십 조건, 사용 기간까지
   공식 출처 기준으로 정리합니다."
  17px Regular, #6B6B6B
  margin-top: 16px

통계 레이블 (선택):
  "현재 브랜드 42개 · 혜택 68개 공식 확인 완료"
  13px Regular, #A3A3A3
  margin-top: 24px
```

### 검색창

```text
width:         100% (max-width 560px, 중앙 정렬)
height:        52px
border:        1.5px solid #EAEAE6
border-radius: 12px
background:    #FFFFFF
padding:       0 16px 0 44px  (좌측 돋보기 아이콘)

focus:         border-color #3B6FE8, box-shadow 0 0 0 3px #EEF3FD

placeholder:   "브랜드명 검색 — 스타벅스, 올리브영, CGV..."
               #A3A3A3

margin-top:    40px
```

### 빠른 필터 (Quick Filter Chips)

```text
레이아웃:     가로 스크롤 가능한 칩 줄 (모바일에서 overflow-x: auto)
margin-top:   20px
gap:          8px

칩 스펙:
  height:          36px
  padding:         0 14px
  border-radius:   999px
  border:          1.5px solid #EAEAE6
  font-size:       13px
  font-weight:     500
  color:           #4A4A4A
  background:      #FFFFFF

  selected:
    background:    #EEF3FD
    border-color:  #3B6FE8
    color:         #3B6FE8

칩 목록:
  전체 / 무료만 / 앱 없이 가능 / 생일월 / 카페 / 외식 / 영화관 / 뷰티
```

### 인기 생일 혜택 섹션

```text
섹션 헤딩 스펙 (공통):
  font-size:     20px
  font-weight:   700
  color:         #1A1A1A
  margin-bottom: 24px
  옆에 작은 레이블: "조회수 기준" — 12px, #A3A3A3

레이아웃:
  데스크탑: 4열 카드 그리드
  모바일:   가로 스크롤 (카드 너비 260px 고정)
```

### 카테고리 바로가기

```text
레이아웃:     6~8개 아이콘 + 텍스트 타일, 2행 또는 1행 가로 스크롤
타일 크기:    80px × 80px (데스크탑), 64px × 64px (모바일)
배경:         #FFFFFF
border:       1px solid #EAEAE6
border-radius: 12px
아이콘:       이모지 또는 line 아이콘 24px (컬러 없이 단색)
텍스트:       12px Medium, #4A4A4A
```

### 최근 업데이트 섹션

```text
레이아웃:     리스트 형태, 카드 아닌 행(row) 스타일
              브랜드명 + 혜택 한 줄 요약 + 확인일
border-bottom: 1px solid #EAEAE6 (각 행)
padding:      16px 0
font-size:    14px
```

---

## 7. 브랜드 목록 페이지

### 필터 패널

```text
데스크탑:
  좌측 사이드바 (너비 220px, sticky)
  섹션별로 구분: 카테고리 / 혜택 유형 / 조건 / 기간
  각 항목: 체크박스 + 레이블, 14px

모바일:
  상단 "필터" 버튼 → 바텀 드로어(bottom sheet)로 표시
  드로어 높이: 80vh
  하단 고정: "적용하기" Primary 버튼

선택된 필터 요약:
  필터 버튼 옆에 칩으로 표시 (삭제 가능)
  예: [카페 ×] [무료 ×] [앱 불필요 ×]
```

### 정렬 드롭다운

```text
위치:         브랜드 목록 우측 상단
크기:         height 36px
border:       1px solid #EAEAE6
border-radius: 8px
font-size:    13px
```

### 브랜드 카드

```text
크기:
  데스크탑:   너비 약 260px, 높이 auto (최소 160px)
  모바일:     너비 calc(50% - 6px), 높이 auto

배경:          #FFFFFF
border:        1px solid #EAEAE6
border-radius: 12px
padding:       20px
shadow:        --shadow-card

hover 상태:
  border-color: #D0D8F0
  shadow:       --shadow-hover
  transition:   all 150ms ease

구성 (위 → 아래):
  ① 브랜드 이니셜 뱃지 + 카테고리 레이블
  ② 브랜드명 (18px Semibold)
  ③ 혜택 한 줄 요약 (14px Regular, 2줄 말줄임)
  ④ 조건 배지 2~3개 (pill)
  ⑤ 구분선
  ⑥ 최근 확인일 + 공식 확인 배지

이니셜 뱃지:
  크기:         40px × 40px
  border-radius: 10px
  배경:         #F4F4F2
  폰트:         16px Bold, #4A4A4A
  (브랜드명 첫 글자)
```

---

## 8. 브랜드 상세 페이지

### 인포바 (상단 고정 정보 띠)

```text
배경:         #F0FDF4
border:       1px solid #D1FAE5
border-radius: 10px
padding:      12px 16px
margin-bottom: 24px

내용:
  좌측: ✓ 공식 확인 완료  (14px Medium, #16A34A)
  우측: 최근 확인일 2026.04.28  (13px, #6B6B6B)
        공식 출처 바로가기 →  (13px, #3B6FE8, 링크)

재확인 필요 상태일 때:
  배경: #FFFBEB
  border-color: #FDE68A
  텍스트: ⚠ 정보 재확인 중  (#D97706)
```

### 페이지 헤딩 영역

```text
브랜드명:        28px Bold, #1A1A1A
카테고리 레이블: 13px Medium, #3B6FE8 (상단)
혜택 한 줄 요약: 17px Regular, #4A4A4A, margin-top 8px
조건 배지 모음:  margin-top 12px, gap 6px
```

### 핵심 조건 테이블

```text
레이아웃:    2열 테이블 (레이블 | 값)
             또는 카드형 그리드 (모바일 적합)

배경:        #F8F8F6
border-radius: 12px
padding:     24px

행 예시:
  혜택 유형     │  무료 음료 쿠폰
  앱 필요       │  ✓ 필요 (스타벅스 앱)
  회원가입      │  ✓ 필요
  멤버십 등급   │  골드 이상
  사용 가능 기간│  생일 당일 + 전후 7일
  최소 구매     │  없음

레이블: 13px Medium, #6B6B6B
값:     14px Regular, #1A1A1A
행 구분: border-bottom 1px solid #EAEAE6
마지막 행: border-bottom 없음
```

### 받는 방법 섹션

```text
헤딩:      "받는 방법"  16px Semibold
배경:      #FFFFFF
border:    1px solid #EAEAE6
border-radius: 12px
padding:   24px

내용: 단계별 설명 (순서 있는 텍스트)
     각 단계: 숫자 뱃지(24px 원) + 설명 텍스트
     숫자 뱃지: 배경 #EEF3FD, 텍스트 #3B6FE8, 14px Bold
```

### 주의사항 섹션

```text
배경:      #FFFBEB
border-left: 3px solid #FCD34D
border-radius: 0 8px 8px 0
padding:   16px 20px
font-size: 14px
color:     #78350F
```

### 공식 출처 링크 버튼

```text
스타일:    Secondary 버튼 (border, 아이콘 포함)
아이콘:    외부 링크 아이콘 14px
텍스트:    "공식 멤버십 페이지에서 확인하기"
width:     100% (모바일), auto (데스크탑)
```

### 면책 문구

```text
위치:      페이지 최하단, 관련 브랜드 위
font-size: 13px
color:     #A3A3A3
border-top: 1px solid #EAEAE6
padding-top: 24px
line-height: 1.8
```

### 관련 브랜드 (하단)

```text
헤딩:    "비슷한 혜택이 있는 브랜드"
레이아웃: 가로 스크롤 카드 3~4개
카드:    간소화 버전 (이니셜 + 브랜드명 + 배지 1개)
```

---

## 9. 카테고리 페이지

```text
페이지 헤딩:   "카페 생일 혜택 모음"  28px Bold
설명 텍스트:   2~3줄, 16px Regular, #6B6B6B

대표 브랜드 TOP 배너:
  배경:         #EEF3FD
  border-radius: 12px
  padding:       24px
  레이아웃:      가로 나열 3~5개 브랜드 이니셜 카드
  컴팩트 카드:   60px × 60px 이니셜 + 브랜드명 + 대표 배지 1개

필터 + 리스트:  /brands 페이지와 동일 구성, 카테고리 자동 선택 상태

관련 태그 영역:
  하단 배치, pill 칩 나열
  예: #무료 #앱불필요 #생일월 #조건없음
  배경: #F4F4F2, 14px, 클릭 시 /tags/[slug]로 이동
```

---

## 10. 태그 페이지

```text
페이지 헤딩:  "앱 설치 없이 받을 수 있는 생일 혜택"  24px Bold

태그 설명 블록:
  배경:         #F8F8F6
  border-radius: 12px
  padding:       24px
  font-size:     15px, line-height 1.8
  블로그형 설명 2~4줄 + 혜택 리스트

관련 카테고리:
  헤딩: "카테고리별 보기"
  타일: 카테고리 아이콘 + 이름, 가로 나열

관련 태그:
  현재 태그와 연관된 다른 태그 칩 나열
```

---

## 11. 정보 수정 제보 페이지

```text
컨테이너:   max-width 560px, 중앙 정렬
배경:       #FFFFFF
border:     1px solid #EAEAE6
border-radius: 16px
padding:    40px (모바일: 24px)

폼 구성:

  브랜드명 입력:
    type: text + 자동완성 드롭다운 (등록된 브랜드 검색)
    placeholder: "브랜드명을 입력하거나 선택하세요"

  제보 유형 선택:
    pill 버튼 선택 (체크박스 대신)
    선택 전: border #EAEAE6, bg #FFFFFF
    선택 후: border #3B6FE8, bg #EEF3FD, text #3B6FE8

  제보 내용:
    textarea, min-height 120px
    border-radius: 10px

  참고 링크:
    type: url, placeholder "https://..."

  이메일 (선택):
    우측에 "(선택)" 레이블 명시
    도움될 경우 답변드린다는 안내 문구 포함

제출 버튼:
  "제보하기"  Primary 버튼, 전체 너비

제출 후:
  버튼을 성공 메시지로 교체 (페이지 이동 없음)
  "제보가 접수됐습니다. 감사합니다." — 인라인 표시
```

---

## 12. 관리자 페이지

```text
관리자 UI는 예쁘지 않아도 된다. 기능 우선.
배경: #F4F4F2
사이드바 + 콘텐츠 영역 구성
```

### 관리자 대시보드

```text
카드 4개 가로 배열 (모바일: 2×2):
  게시 중 혜택 수         (초록)
  검수 필요 혜택 수       (주황)
  90일 이상 미확인 수     (빨강)
  최근 7일 제보 수        (파랑)

카드 스펙:
  배경:          #FFFFFF
  border-radius: 10px
  padding:       20px
  숫자:          32px Bold
  레이블:        13px, #6B6B6B
```

### 브랜드 관리

```text
테이블 뷰:
  컬럼: 브랜드명 / 카테고리 / 혜택 수 / 상태 / 최근 확인일 / 액션
  행 hover: 배경 #F8F8F6

등록/수정 폼:
  라벨 + 입력 필드 단순 구성
  상태 변경: 드롭다운 선택
```

### 혜택 관리

```text
테이블 컬럼:
  브랜드명 / 혜택명 / 검수상태 뱃지 / 최근 확인일 / 제보 수 / 액션

검수 상태 뱃지:
  DRAFT         bg: #F4F4F2  text: #4A4A4A
  NEEDS_CHECK   bg: #FFFBEB  text: #D97706
  VERIFIED      bg: #EEF3FD  text: #3B6FE8
  PUBLISHED     bg: #F0FDF4  text: #16A34A
  EXPIRED       bg: #FEF2F2  text: #DC2626
```

### 제보 관리

```text
테이블 컬럼: 브랜드명 / 유형 / 내용 미리보기 / 접수일 / 상태 / 바로가기

상태 구분:
  미처리:  주황
  처리완료: 초록
  반려:    회색
```

---

## 13. 디자인 원칙 요약

```text
① 정보가 먼저다
   텍스트 계층과 배지가 전부. 화려한 그래픽 없음.

② 여백이 신뢰다
   빽빽한 레이아웃보다 넉넉한 여백이 더 정갈해 보인다.

③ 포인트 컬러 1개
   #3B6FE8 하나만. 브랜드 컬러를 페이지에 가져오지 않는다.

④ 배지로 빠르게 비교
   텍스트 설명보다 조건 배지를 먼저 눈에 띄게.

⑤ 모바일 우선
   필터, 카드, 상세 모두 모바일에서 먼저 설계하고 데스크탑으로 확장.

⑥ 광고처럼 보이지 않기
   일러스트, 배너, 과도한 CTA, 팝업 없음.
   공식 확인 배지와 출처 링크가 신뢰의 시각적 앵커.
```

---

## 14. 처음부터 하지 말 것

- 복잡한 온보딩
- 회원가입 유도
- 개인 생일 입력 모달
- AI 추천 배너
- 광고 과다 배치
- 앱 다운로드 유도
- 브랜드 공식 서비스처럼 보이는 디자인
- 다크모드 (MVP에서는 불필요)
- 브랜드별 포인트 컬러 적용
- 스켈레톤 로더 과다 사용 (단순 스피너로 충분)