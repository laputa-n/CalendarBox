<div align="center">

### 📆 Calendar Box (캘박)
공유 캘린더 기반 일정 관리 서비스

</div>

## 목차
- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
  - [홈](#홈)
  - [캘린더](#-캘린더)
  - [일정](#️%EF%B8%8F일정)
  - [친구](#-친구)
  - [통계 및 분석](#-통계-및-분석)
- [기술 스택](#기술-스택)
- [서비스 아키텍처](#서비스-아키텍처)
- [역할](#역할)
- [확장성](#확장성)
  
## 프로젝트 소개

**Calendar Box**는 여러 모임과 일정을 효율적으로 관리하기 위한 

**공유 캘린더 기반 일정 관리 서비스**입니다.

개인 일정부터 그룹 모임까지 하나의 플랫폼에서 관리할 수 있으며,

일정에 **참여자·장소·첨부파일·지출·통계** 등 다양한 하위 리소스를 결합하여

기억과 기록을 함께 남길 수 있습니다.

---

## 주요 기능

### 🏠홈

- 상단 -  오늘 일정 수, 친구 수, 캘린더 수 표시
- 중앙 -  내 모든 캘린더에 등록된 종합 일정이 표시된 캘린더 뷰
- 우측 -  오늘 일정 정보&오늘 날짜 제외 예정된 일정 정보 표시
- 좌측 -  네비게이터 원하는 페이지로 이동

![스크린샷 2026-02-10 185311.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_185311.png)

### 📅 캘린더

그룹별 캘린더 뷰 분리

![스크린샷 2026-02-10 154755.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_154755.png)

![스크린샷 2026-02-10 154819.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_154819.png)

그룹 캘린더 **멤버** 기능

![스크린샷 2026-02-10 160401.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_160401.png)

 **공개 범위** 설정으로 안전한 일정 관리

![스크린샷 2026-02-10 165504.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_165504.png)

캘린더 **상세 정보** 및 **히스토리**

![스크린샷 2026-02-10 160315.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_160315.png)

![스크린샷 2026-02-10 160629.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_160629.png)

### ☀️일정

구성: 제목, 시작 시간, 종료 시간, 테마(색상), 메모
하위리소스: 장소, 참가자, 투두, 반복, 리마인더, 링크, 지출/세부 지출, 첨부파일(이미지/파일/영수증)

장소 검색으로 편리한 일정 장소 등록 및 장소 순서 변경 가능

![스크린샷 2026-02-10 174025.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_174025.png)

![스크린샷 2026-02-10 174038.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_174038.png)

![스크린샷 2026-02-10 184502.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_184502.png)

일정 지역 입력 시 **장소 추천**

![스크린샷 2026-02-10 175128.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_175128.png)

![스크린샷 2026-02-10 175226.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_175226.png)

다양한 **참가자** 추가 기능

![스크린샷 2026-02-10 175813.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_175813.png)

**반복 기능**으로 효율적인 일정 등록/수정/삭제

![스크린샷 2026-02-10 180112.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_180112.png)

![스크린샷 2026-02-10 180138.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_180138.png)

![스크린샷 2026-02-10 180150.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_180150.png)

**첨부 파일** 업로드/미리보기/다운로드

![스크린샷 2026-02-10 181135.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_181135.png)

 **색상**으로 구분

![스크린샷 2026-02-10 181625.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_181625.png)

![스크린샷 2026-02-10 181651.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_181651.png)

일정별 **지출 관리** 기능

![스크린샷 2026-02-10 182943.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_182943.png)

![스크린샷 2026-02-10 182735.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_182735.png)

**영수증 업로드**를 통한 **지출/지출 세부 항목 등록** 기능

![스크린샷 2026-02-10 183652.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_183652.png)

![스크린샷 2026-02-10 183709.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_183709.png)

그 외 다양한 일정 하위 리소스 기능(투두, 리마인더, 링크)

![스크린샷 2026-02-10 181857.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_181857.png)

![스크린샷 2026-02-10 182105.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_182105.png)

일정 관리 탭에서 캘린더별 **일정 목록 조회** 가능 & 일정 **검색** 가능 & 일정 초대 **수락/거절** 가능

![스크린샷 2026-02-10 184751.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_184751.png)

![스크린샷 2026-02-10 184825.png](docs/images/readme/67d84a0d-b781-42f9-9f57-54c15d68e98d.png)

### 👥 친구

**친구 목록**

![스크린샷 2026-02-10 235233.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_235233.png)

이름/이메일/전화번호로 유저 **검색** 및 **친구 요청**

![스크린샷 2026-02-10 235535.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_235535.png)

메일 주소 입력으로 바로 친구 요청 가능

![스크린샷 2026-02-10 235626.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_235626.png)

 **받은 친구 요청** 조회 및 수락/거절 가능

![스크린샷 2026-02-10 235739.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_235739.png)

**보낸 친구 요청** 조회 및 취소 가능

![스크린샷 2026-02-10 235819.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-10_235819.png)

### 📊 통계 및 분석

사람 통계 - 만난 횟수 순으로 **요약 및 분석** 정보 제공

![스크린샷 2026-02-11 000208.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-11_000208.png)

장소 통계 - 방문 횟수 순으로 **요약 및 분석** 정보 제공

![스크린샷 2026-02-11 000510.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-11_000510.png)

월별 통계 - **월별 스케줄 추이** 제공 및 스케줄 수 확인 가능

![스크린샷 2026-02-11 000826.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-11_000826.png)

요일별 통계 - **요일별 스케줄 분포** 및 스케줄 수 확인 가능

![스크린샷 2026-02-11 000917.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-11_000917.png)

시간대별 통계 - **요일-시간대별** 스케줄 분포 제공

![스크린샷 2026-02-11 001056.png](docs/images/readme/%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2026-02-11_001056.png)

---

## 기술 스택

### Frontend

- React
- Tailwind CSS
- JavaScript
- Thymeleaf

### Backend

- Java 21
- Spring Boot
- Spring Data JPA

### ML

- Python
- FastAPI
- sentence-transformers

### Database / Infrastructure

- Nginx
- Docker
- PostgreSQL 16 (+ pgvector)
- Redis
- RabbitMQ
- AWS EC2/S3
- GitHub Actions (CI/CD)
- Swagger
- Flyway (DB Migration)

### External APIs

- Kakao OAuth
- Naver Local Search
- Naver CLOVA OCR
- Gemini-flash

---

## 서비스 아키텍처

![제목 없는 다이어그램 (4).jpg](docs/images/readme/811ce37e-e23b-413f-89a5-91c376549eb9.png)

---

## 역할

🌱남승혁 - PM / Infra / Backend

❤️마강현 - Frontend / Design

---

## 확장성

공개 그룹 생성 및 가입 요청/활동 등으로 확장 가능(오픈카카오톡)

공개 일정 등록 및 조회/참가/활동 등으로 커뮤니티 기능으로 확장 가능(당근모임)
