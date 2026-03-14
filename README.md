# Logmeet Backend

Logmeet는 프로젝트, 일정, 회의록을 한 곳에서 관리하는 협업 서비스입니다.  
이 저장소는 Logmeet의 Spring Boot 백엔드로, 인증, 프로젝트 관리, 일정 관리, 회의록 처리, 검색 기능을 담당합니다.

## Project Links

- 기능 소개 및 시연 영상: [Logmeet 소개 페이지](https://www.notion.so/Logmeet-13f078c2c97480f6897adce0fbad689f)

## Project Highlights

- JWT 기반 회원가입, 로그인, 로그아웃과 Refresh Token 관리
- 프로젝트, 일정, 회의록 도메인 모델링과 주요 API 설계
- Flask AI 서버와 연동한 음성 처리, 이미지 OCR, 텍스트 요약 흐름
- Elasticsearch 기반 회의록 검색과 최근 검색 기록 10개 제한
- Object Storage 업로드, Pre-signed URL 발급, 30분 경과 임시 회의록 정리 스케줄러
- JaCoCo 90% 기준, PMD, SpotBugs를 적용한 품질 관리

## Core Features

### Authentication
- 회원가입, 로그인, 로그아웃 API를 제공합니다.
- JWT 기반 인증을 사용하며, 인증이 필요한 요청은 필터에서 토큰을 검증합니다.

### Project Management
- 프로젝트 생성, 수정, 삭제, 즐겨찾기, 초대 코드 발급 기능을 제공합니다.
- 프로젝트 참여, 멤버 추방, 리더 위임, 프로젝트 탈퇴 흐름을 지원합니다.

### Schedule Management
- 프로젝트별 일정과 사용자별 일정을 조회할 수 있습니다.
- 일정 생성, 수정, 삭제를 포함한 일정 관리 기능을 제공합니다.

### Minutes Processing
- 회의록을 수동으로 입력하거나 음성 파일, 이미지 파일 기반으로 생성할 수 있습니다.
- Object Storage 업로드용 Pre-signed URL을 발급합니다.
- Flask AI 서버와 연동해 음성 처리, OCR, 텍스트 요약을 수행합니다.
- 임시 회의록을 생성한 뒤 정식 회의록으로 저장하는 흐름을 지원합니다.

### Search
- 회의록 검색 기능을 제공합니다.
- 최근 검색 기록 저장, 조회, 삭제 기능을 지원합니다.

## Implementation Highlights

### 1. Authentication and Access Control
- JWT 기반 인증과 사용자 검증 흐름으로 구성되어 있습니다.
- 로그인 시 Access Token과 Refresh Token을 발급하고, 로그아웃 시 Refresh Token을 제거합니다.
- 인증이 필요한 요청은 필터와 사용자 주입 로직으로 처리합니다.

### 2. Collaboration Domain Backend
- `Project`, `Schedule`, `Minutes`, `UserProject` 중심으로 협업 도메인을 나눠 설계했습니다.
- 프로젝트 멤버십과 역할 검증을 서비스 계층에서 일관되게 처리하도록 구성되어 있습니다.

### 3. AI Processing Integration
- 음성 파일은 Flask 서버의 `/process_audio`로 전달해 텍스트로 변환합니다.
- 이미지 파일은 `/process_image`로 전달해 OCR 처리합니다.
- 텍스트 요약은 `/summarize_text`와 연동합니다.
- 파일 업로드 이후 임시 회의록을 만들고 후속 처리 결과를 반영하는 구조로 구성되어 있습니다.

### 4. Search Support
- 회의록 검색 기능을 위해 Elasticsearch를 사용합니다.
- 최근 검색 기록을 별도 도메인으로 관리하고, 사용자별 최근 10개까지만 유지합니다.

### 5. Storage and Operations
- Object Storage 업로드를 위해 Pre-signed URL 발급 기능을 제공합니다.
- 30분이 지난 임시 회의록을 정리하는 스케줄러를 운영합니다.

### 6. Quality Bar
- `./gradlew check`에 JaCoCo 커버리지 검증을 연결했습니다.
- 라인 커버리지 최소 기준은 `90%`입니다.
- PMD와 SpotBugs를 함께 적용해 정적 분석 기준을 유지합니다.

## Tech Stack

### Backend
- Java
- Spring Boot 3.2.5
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation

### Data and Search
- MySQL
- H2
- Elasticsearch

### Auth, API, Storage
- JWT
- Springdoc OpenAPI
- Naver Cloud Object Storage

### External Integration
- Flask AI Server

### Quality
- JUnit 5
- Spring Security Test
- JaCoCo
- PMD
- SpotBugs

## API Summary

### Auth
- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/logout`

### Projects
- `POST /projects/new`
- `GET /projects/{projectId}`
- `GET /projects/project-list`
- `GET /projects/bookmark-list`
- `PUT /projects/{projectId}`
- `PUT /projects/{projectId}/bookmark`
- `DELETE /projects/expel`
- `DELETE /projects/{projectId}`
- `PUT /projects/{projectId}/leader`
- `DELETE /projects/{projectId}/leave`
- `GET /projects/{projectId}/invite-code`
- `POST /projects/join`

### Schedule
- `POST /schedule/new`
- `PUT /schedule/{scheduleId}`
- `GET /schedule/{scheduleId}`
- `GET /schedule/{projectId}/schedule-list`
- `GET /schedule/{projectId}/schedules`
- `GET /schedule/users/schedule-list`
- `GET /schedule/users/schedules`
- `DELETE /schedule/{scheduleId}`

### Minutes
- `GET /minutes/generate-pre-signed-url`
- `POST /minutes/new`
- `POST /minutes/upload-content`
- `PUT /minutes/update-info`
- `POST /minutes/{minutesId}/summarize-text`
- `GET /minutes/{minutesId}`
- `GET /minutes/minutes-list`
- `GET /minutes/{projectId}/minutes-list`
- `DELETE /minutes/{minutesId}`

### Search
- `POST /search`
- `GET /search/history`
- `POST /search/{minutesId}/history`
- `DELETE /search/history/{historyId}`

## Project Structure

```text
src/main/java/org/focus/logmeet
├── common         # 공통 응답, 예외 처리, 유틸
├── config         # Swagger, RestTemplate, Elasticsearch 설정 등
├── controller     # API 엔드포인트
├── domain         # JPA 엔티티, enum, Elasticsearch document
├── repository     # JPA / Elasticsearch repository
├── security       # JWT 인증, 필터, argument resolver
└── service        # 비즈니스 로직
```

## Local Setup

이 프로젝트는 MySQL, Elasticsearch, Flask AI 서버, Object Storage 설정이 준비되어야 전체 기능을 사용할 수 있습니다.

### Required Environment Variables

| Name | Description |
| --- | --- |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | MySQL 사용자명 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `JWT_SECRET_KEY` | JWT 서명 키 |
| `NCLOUD_ACCESS_KEY` | Object Storage 접근 키 |
| `NCLOUD_SECRET_KEY` | Object Storage 비밀 키 |

아래 설정도 실제 사용 가능한 값으로 맞춰져 있어야 합니다.

- `flask.server.url`
- `spring.elasticsearch.uris`
- `spring.elasticsearch.username`
- `spring.elasticsearch.password`

```bash
./gradlew bootRun
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Quality Checks

```bash
./gradlew test
./gradlew check
```
