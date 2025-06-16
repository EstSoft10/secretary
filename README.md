# 🧠 A:ssistant - 당신의 일정과 함께하는 AI 비서

사용자의 일정을 AI가 도와주는 스마트 캘린더 & 챗봇 웹 서비스입니다.  
주간 일정 관리부터 자연어 질문, 유튜브 요약까지 — **당신의 하루를 더 편리하게**.

---

<details>
<summary>📌 목차</summary>

1. [프로젝트 소개](#-프로젝트-소개)  
2. [서비스 특징](#-서비스-특징)  
3. [주요 기능](#-주요-기능)  
4. [기술 스택](#-기술-스택)  
5. [프로젝트 구조](#-프로젝트-구조)  
6. [테이블 리스트](#-테이블-리스트)  
7. [기능 명세서](#-기능-명세서)  
8. [화면 설계](#-화면-설계)  
9. [API 명세서](#-api-명세서)  
10. [배포 및 CI/CD](#-배포-및-ci-cd)  
11. [암호화 설정](#-암호화-설정)  
12. [아키텍처 다이어그램](#-아키텍처-다이어그램)

</details>

---

## 🪧 프로젝트 소개

> 일정 관리에 AI를 결합한 웹 기반 비서 서비스입니다.  
> 주간 캘린더, 챗봇, 유튜브 자막 요약 등 다양한 기능을 하나의 플랫폼에서 제공합니다.

---

## ✨ 서비스 특징

- **주간 일정 관리**: 오늘 기준 ±3일 주간 뷰 구성
- **AI 챗봇**: 자연어 질문 응답 및 마크다운 기반 출력
- **YouTube 요약**: 자막 분석 후 요약 및 타임스탬프 클릭 이동
- **일정 기반 추천 질문**: 일정 등록 후 연관된 AI 질문 자동 제안
- **AI 프롬프트 카드 캐싱**: 6시간 단위로 미리 AI 응답 캐싱 및 분산 처리
- **이메일 내보내기**: `.ics` 파일로 일정 내보내기 (SMTP 연동)

---

## 💡 주요 기능

| 기능             | 설명 |
|------------------|------|
| 캘린더 UI         | FullCalendar 기반 주간 뷰 구성 |
| 일정 등록 모달     | 날짜 클릭 시 모달로 일정 등록 |
| 추천 질문 팝업     | 일정 저장 시 AI 추천 문장 표시 |
| 챗봇 대화         | 사용자 질문에 AI 응답 + 기록 저장 |
| 유튜브 요약       | 영상 링크로 자막 추출 후 요약/출력 |
| 대화 기록 관리     | 날짜별 그룹화 + 최신순 정렬 + 이어 대화 |
| 일정 내보내기     | `.ics` 생성 → 입력한 이메일로 발송 |

---

## 🛠 기술 스택

### Java 기반
- Java 17
- Spring Boot 3.5.0
- Spring Security / OAuth2 Client
- Spring Data JPA / JDBC
- Spring Mail, Thymeleaf
- WebFlux (WebClient 기반 비동기 호출)
- Cache: Spring Boot Cache
- Jasypt (암호화)
- Biweekly (.ics 일정 생성)
- Log4jdbc (SQL 로깅)

### Python 기반 (FastAPI)
- FastAPI, uvicorn
- yt-dlp (YouTube 자막 추출)
- requests, pydantic, urllib

### 기타
- MySQL (AWS RDS)
- AWS EC2 (Ubuntu 22.04)
- GitHub Actions + S3 + CodeDeploy
- Certbot (Let's Encrypt HTTPS)

---

## 📁 프로젝트 구조

```
/secretary
 ┣ /controller
 ┣ /service
 ┣ /repository
 ┣ /domain
 ┣ /dto
 ┣ /config
 ┣ /static
 ┗ /templates
```

---

## 🧾 테이블 리스트 (요약)

| 테이블명           | 설명               |
|--------------------|--------------------|
| member             | 사용자 정보        |
| schedule           | 일정 정보          |
| ai_conversation    | AI 대화 세션       |
| ai_message         | AI 질문 및 응답    |
| subtitle_summary   | 유튜브 요약 결과   |

---

## 📋 기능 명세서

> 별도 문서로 정리되어 있으며, 요청 시 공유 가능

---

## 🖼 화면 설계

> 주요 화면 (캘린더, 챗봇, 요약 등) 캡처 포함  
> Figma 또는 실제 UI 이미지로 제공 가능

---

## 📡 API 명세서

| 엔드포인트              | 설명                     |
|-------------------------|--------------------------|
| `/api/schedule`         | 일정 CRUD                |
| `/api/conversation/...` | 챗봇 대화 관리           |
| `/extract-and-summary`  | 유튜브 자막 요약 (FastAPI) |

---

## 🚀 배포 및 CI/CD

### ▶ 배포 구성

- **EC2**: Spring Boot 애플리케이션 `.jar` 실행
- **FastAPI 서버**: Python 기반 Uvicorn 서버 (별도 포트)
- **S3 + CodeDeploy**: GitHub Actions에서 빌드 후 S3 업로드 → EC2 배포
- **Certbot + HTTPS**: Nginx + Let’s Encrypt 인증서 적용 (443 포트)
- **Jasypt 암호화**: 민감 정보는 Jasypt로 암호화, EC2에서는 SSM Parameter Store로 복호화

### ▶ GitHub Actions

- `release` 브랜치 push 시:
  - 전체 Gradle 빌드 수행
  - `.jar` 및 설정 파일을 S3에 업로드
  - CodeDeploy 트리거 → EC2 배포 후 `start.sh` 실행
- GitHub Secrets로 환경변수 관리 (`JASYPT_ENCRYPTOR_PASSWORD` 등)

---

## 🔐 암호화 설정

- `application.properties` 내 민감한 키(`mail.password`, `oauth.client-secret` 등)는 Jasypt로 암호화
- 암호화 방법:
  ```bash
  java -jar jasypt-encryptor.jar input=비밀번호 key=secretary
  → ENC(암호문)
  ```
- 복호화:
  - EC2 서버 환경변수로 `JASYPT_ENCRYPTOR_PASSWORD=secretary` 설정
  - 또는 `-Djasypt.encryptor.password=secretary` 로 실행 시 주입
- EC2 자동 배포 시 `appspec.yml` + `start.sh` 내 환경변수로 주입 처리

---

## 🏗 아키텍처 다이어그램

```
사용자
   ↓
[Spring Boot App] -- REST --> [FastAPI 서버 (Python)]
   ↓                               ↑
[MySQL RDS]                   [yt-dlp + OpenAI]
   ↓
[AWS S3 + CodeDeploy + EC2] ← GitHub Actions (CI/CD)
```

- 프론트엔드는 Thymeleaf 기반 SSR
- AI 응답은 WebClient → FastAPI → 자막 추출 후 요약
- 결과는 사용자와의 대화 형태로 출력 및 저장

---
