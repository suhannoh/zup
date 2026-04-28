# 08. 개발 환경 및 Docker 가이드

## 1. 로컬 개발 방식

개발 시에는 아래 방식을 기본으로 한다.

- PostgreSQL / Redis: Docker Compose
- Backend: IntelliJ 또는 Gradle bootRun
- Frontend: npm run dev

즉, 개발 중에는 backend/frontend를 Docker로 띄우지 않아도 된다.

## 2. 루트 구조

```text
zup/
├── backend/
├── frontend/
├── infra/
├── docs/
├── scripts/
├── docker-compose.dev.yml
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## 3. 환경 변수

루트에 `.env` 파일을 만든다.

`.env.example`을 복사해서 시작한다.

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

## 4. 개발용 Docker Compose

파일:

```text
docker-compose.dev.yml
```

서비스:

- postgres
- redis

기본 값:

```env
POSTGRES_DB=zup
POSTGRES_USER=zup
POSTGRES_PASSWORD=zup
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zup
SPRING_DATASOURCE_USERNAME=zup
SPRING_DATASOURCE_PASSWORD=zup
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

실행:

```bash
docker compose -f docker-compose.dev.yml up -d
```

중지:

```bash
docker compose -f docker-compose.dev.yml down
```

볼륨까지 삭제:

```bash
docker compose -f docker-compose.dev.yml down -v
```

## 5. Backend 실행

```bash
cd backend
./gradlew bootRun
```

Windows:

```powershell
cd backend
.\gradlew.bat bootRun
```

기본 포트:

```text
8080
```

Health check:

```http
GET http://localhost:8080/api/v1/health
```

예상 응답:

```json
{
  "success": true,
  "data": {
    "status": "OK"
  },
  "message": "healthy"
}
```

## 6. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

기본 포트:

```text
3000
```

접속:

```text
http://localhost:3000
```

## 7. Frontend 환경 변수

`frontend/.env.local` 또는 루트 `.env`에서 관리한다.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## 8. 운영용 Docker Compose

파일:

```text
docker-compose.yml
```

서비스:

- postgres
- redis
- backend
- frontend

초기에는 골격만 있어도 된다.

개발 우선순위는 `docker-compose.dev.yml`이다.

## 9. Nginx

Nginx는 배포 단계에서 추가한다.

예정 구조:

```text
/api -> backend:8080
/    -> frontend:3000
```

## 10. GitHub Actions

초기에는 수동 배포로 충분하다.

추후:

- backend build
- frontend build
- Docker image build
- EC2 배포
- docker compose restart

## 11. 로컬 초기화 순서

1. `.env.example`을 `.env`로 복사
2. `docker compose -f docker-compose.dev.yml up -d`
3. backend 실행
4. frontend 실행
5. health check
6. 메인 페이지 접속

## 12. 자주 발생할 문제

### PostgreSQL 연결 실패

확인:

- Docker 실행 여부
- 포트 5432 충돌 여부
- DB 이름/계정/비밀번호 일치 여부

### Redis 연결 실패

확인:

- Redis 컨테이너 실행 여부
- 포트 6379 충돌 여부

### CORS 오류

확인:

- backend CORS 허용 origin
- frontend 포트
- `CORS_ALLOWED_ORIGINS=http://localhost:3000`

### Next.js API URL 오류

확인:

- `NEXT_PUBLIC_API_BASE_URL`
- 브라우저 콘솔
- backend 실행 여부
