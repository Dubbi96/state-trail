# StateTrail (Monorepo)

`key_idea.md`를 기반으로 한 **UI 상태공간(State Space) 컴파일러 + 그래프 기반 테스트 생산/운영** 플랫폼의 FE/BE 모노레포입니다.

## 디렉토리

- `frontend/`: Next.js(App Router) + TypeScript + Tailwind 기반 UI
- `backend/`: Spring Boot 3 + JPA(PostgreSQL) 기반 API 서버
- `docker-compose.yml`: 로컬 개발용 PostgreSQL/Redis/MinIO

## 로컬 개발 빠른 시작

### 1) 인프라(PostgreSQL/Redis/MinIO)

```bash
cd /Users/gangjong-won/Dubbi/StateTrail
docker compose up -d
```

### 2) Backend

```bash
cd /Users/gangjong-won/Dubbi/StateTrail/backend
./gradlew playwrightInstall   # (선택) BROWSER_* 전략 사용 시 1회 설치 권장
./gradlew bootRun
```

> Gradle wrapper jar는 바이너리 커밋을 피하기 위해 포함하지 않았습니다.  
> 로컬에 Gradle이 없다면 `gradle wrapper`로 생성 후 사용하세요.

### 3) Frontend

```bash
cd /Users/gangjong-won/Dubbi/StateTrail/frontend
npm install
npm run dev
```

## MVP에서 구현되는 API(초기 스캐폴딩)

- Project/AuthProfile/CrawlRun/Graph/Artifact/Flow/TestRun의 최소 CRUD 스켈레톤
- CrawlRun 이벤트 스트리밍: SSE 엔드포인트 뼈대

## 문서

- `key_idea.md`: 컨셉 필라 + E2E Flow + MVP 스펙


