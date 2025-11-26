# CalendarBox 프로젝트 분석

## 전체 구성
- **apps/backend**: Spring Boot 기반 REST API 서버로 일정, 달력, 친구, 알림, 분석 등 도메인을 포함합니다.
- **apps/frontend**: React 19 + React Router 기반 SPA로 인증/일정/통계 UI를 제공합니다.
- **apps/analytics-data**: 일정 데이터 전처리·모델 학습·서빙을 위한 파이프라인(Python, FastAPI)입니다.

## 백엔드(Sprint Boot)
- `BackendApplication`에서 스케줄링/비동기 작업/JPA Auditing을 활성화하여 주기 작업과 엔티티 감사를 지원합니다.
- 일정 API는 생성·수정·삭제·단건 조회·목록 조회·검색을 제공하며, 인증된 사용자를 기반으로 동작합니다.
- 분석 API는 월별 사람/장소 요약과 페이지네이션 목록, 요일-시간대 분포, 월별 스케줄 추이를 제공합니다.

## 프론트엔드(React)
- `App.js`에서 에러/인증/캘린더/일정/알림/친구 컨텍스트를 중첩 공급하여 글로벌 상태를 구성한 뒤 라우터를 렌더링합니다.
- `AppRouter`는 인증 상태와 경로에 따라 로그인/회원완료/대시보드 및 달력·일정·친구·알림·통계·검색·캘린더 보드 페이지를 조건 렌더링합니다.

## 데이터 분석 파이프라인
- `preprocess_data.py`는 일정+라벨 CSV를 결합하고 시간/주말/로그금액/기간 버킷 등의 피처를 생성해 전처리된 데이터셋을 만듭니다.
- `train_model.py`는 제목+장소 텍스트와 시간·기간·금액 수치 피처를 TF-IDF, 표준화 후 RandomForest 분류기로 학습하여 모델 번들을 저장합니다.
- `model_server.py`는 저장된 번들을 로드한 FastAPI 서버로, `/predict`에서 여러 일정의 텍스트+수치 피처를 결합해 카테고리를 예측합니다.
