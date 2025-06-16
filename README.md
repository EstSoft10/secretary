# A:ssistant - 당신의 일정과 함께하는 AI 비서

사용자의 일정을 AI가 도와주는 스마트 캘린더 & 챗봇 웹 서비스입니다.  
주간 일정 관리부터 자연어 질문, 유튜브 요약까지 — **당신의 하루를 더 편리하게**.

---

<details>
<summary>📌 목차</summary>

1. [프로젝트 소개](#-프로젝트-소개)  
2. [서비스 특징](#-서비스-특징)  
3. [주요 기능](#-주요-기능)  
4. [기술 스택](#-기술-스택)
5. [시스템 아키텍처](#-시스템-아키텍처)
6. [프로젝트 구조](#-프로젝트-구조)  
7. [테이블 리스트](#-테이블-리스트)  
8. [기능 명세서](#-기능-명세서)   
9. [API 명세서](#-api-명세서)  
10. [배포 및 CI/CD](#-배포-및-ci-cd)  
11. [암호화 설정](#-암호화-설정)  

</details>

---

## 📝 프로젝트 소개

> 일정 관리에 AI를 결합한 웹 기반 비서 서비스입니다.  
> 주간 캘린더, 챗봇, 유튜브 자막 요약 등 다양한 기능을 하나의 플랫폼에서 제공합니다.

---

## ✨ 서비스 특징

- **주간 일정 관리**: 오늘 기준 ±3일 주간 뷰 구성
- **AI 챗봇**: 자연어 질문 응답 및 마크다운 기반 출력
- **대화형 음성 비서**: 음성으로 일정을 등록, 수정, 삭제하는 대화형 AI 기능
- **YouTube 요약**: 자막 분석 후 요약 및 타임스탬프 클릭 이동
- **일정 기반 추천 질문**: 일정 등록 후 연관된 AI 질문 자동 제안
- **AI 프롬프트 카드 캐싱**: 6시간 단위로 미리 AI 응답 캐싱 및 분산 처리
- **내보내기 및 가져오기**: `.ics` 파일로 일정 내보내기(SMTP 연동) 및 가져오기

---

## 💡 주요 기능

| 기능             | 설명 |
|------------------|------|
| 음성 명령 처리 | Web Speech API로 음성을 인식하고, Gemini API로 자연어를 분석하여 일정 CRUD를 수행하는 다단계 대화형 기능 |
| 캘린더 UI         | FullCalendar 기반 월간 뷰 구성 |
| 일정 등록 모달     | 날짜 클릭 시 모달로 일정 등록 |
| 추천 질문 팝업     | 일정 저장 시 AI 추천 문장 표시 |
| 챗봇 대화         | 사용자 질문에 AI 응답 + 기록 저장 |
| 유튜브 요약       | 영상 링크로 자막 추출 후 요약/출력 |
| 대화 기록 관리     | 날짜별 그룹화 + 최신순 정렬 + 이어 대화 |
| 일정 내보내기     | `.ics` 생성 → 입력한 이메일로 발송 |
| 일정 가져오기     | `.ics` 업로드 → DB에 동기화 |

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

### 배포
- AWS EC2 (Amazon Linux 2)
- MySQL (AWS RDS)
- GitHub Actions + S3 + CodeDeploy
- Certbot (Let's Encrypt HTTPS)

### 외부 API
- 공공데이터 포털 공휴일 정보조회
- OpenWeatherMap API(날씨 정보)
- Geolocation API(현재 위치)
- Web Speech API - SpeechRecognition(음성 인식)
- Google Gemini API: 자연어 이해(NLU) 및 분석
  

---

## 🏗 시스템 아키텍처

서비스 전반 구성 요소 및 요청 흐름을 시각화한 다이어그램입니다.

![final drawio](https://github.com/user-attachments/assets/82c6df28-7ad8-4b11-ba92-d33432c48654)

## 📁 프로젝트 구조

```
/secretary
 ┣ /configuration
 ┣ /controller
 ┣ /service
 ┣ /repository
 ┣ /domain
 ┣ /dto
```

---

## 🧾 테이블 리스트 (요약)

| 테이블명           | 설명               |
|--------------------|--------------------|
| member             | 사용자 정보        |
| schedule           | 일정 정보          |
| ai_conversation    | AI 대화 세션       |
| ai_message         | AI 질문 및 응답    |

---

## 📋 기능 명세서

<img width="754" alt="기능명세 PNG" src="https://github.com/user-attachments/assets/38fdfeba-dd2d-43ad-8595-b579a65a0fec" />

---

## 📡 API 명세서

🔹 AI 검색

| 메서드   | URL                                     | 설명                                     |
| ----- | --------------------------------------- | -------------------------------------- |
| `GET` | `/api`                                  | AIController WebClient 초기화 확인용 (추정)    |
| `GET` | `/search`                               | 사용자의 검색 쿼리를 포함해 `/searchResult`로 리다이렉트 |
| `GET` | `/async-search`                         | 앨런 AI에게 쿼리를 보내고 비동기로 응답 반환             |
| `GET` | `/conversation/{userId}`                | 특정 회원의 모든 대화 내역 조회                     |
| `GET` | `/conversation/detail/{conversationId}` | 특정 대화 ID에 해당하는 모든 메시지 조회               |

🔹 캘린더 (Schedule)

| 메서드      | URL                     | 설명           |
| -------- | ----------------------- | ------------ |
| `GET`    | `/api/schedules`        | 전체 월간 일정 조회  |
| `GET`    | `/api/schedules/day`    | 특정 날짜 일정 조회  |
| `GET`    | `/api/schedules/{id}`   | 일정 단건 조회     |
| `POST`   | `/api/schedules`        | 새 일정 추가      |
| `PUT`    | `/api/schedules/{id}`   | 특정 일정 수정     |
| `DELETE` | `/api/schedules/{id}`   | 특정 일정 삭제     |
| `GET`    | `/api/schedules/counts` | 날짜별 일정 개수 조회 |

🔹 **음성 비서 (Voice Assistant)**

| 메서드 | URL | 설명 |
| :--- | :--- | :--- |
| `POST` | `/api/voice/analyze` | 사용자의 음성(텍스트) 쿼리를 받아 자연어를 분석하고, 대화 상태(Session)에 따라 일정 생성/수정/삭제 작업을 수행하거나 후속 질문을 반환합니다. |

🔹 외부 API

| 메서드   | URL             | 설명                          |
| ----- | --------------- | --------------------------- |
| `GET` | `/weather`      | 위도와 경도를 사용하여 현재 위치 날씨 정보 조회 |
| `GET` | `/api/holidays` | 공휴일 목록 조회                   |

🔹 유튜브 요약

| 메서드    | URL                            | 설명                |
| ------ | ------------------------------ | ----------------- |
| `GET`  | `/youtube-summary`             | 유튜브 요약 결과 페이지     |
| `POST` | `/youtube/extract-and-summary` | 유튜브 자막 추출 및 요약 실행 |

---

## 🚀 배포 및 CI/CD

### ▶ 배포 구성
- **EC2**: Spring Boot 애플리케이션 `.jar` 실행
- **FastAPI 서버**: Python 기반 Uvicorn 서버 (8000 포트)
+ - **Nginx (Reverse Proxy)**: 클라이언트의 HTTPS 요청을 수신하고, 도메인 경로에 따라 Spring Boot(8080) 또는 FastAPI(8000)로 프록시 분기
+ - **SSL 인증서 (Let's Encrypt)**: Certbot으로 자동 발급/갱신, 443 포트 HTTPS 암호화 제공
- **S3 + CodeDeploy**: GitHub Actions에서 빌드 후 S3 업로드 → EC2 배포
- **Jasypt 암호화**: 민감 정보는 Jasypt로 암호화, EC2에서는 SSM Parameter Store로 복호화
+ - **Spring Boot → FastAPI 통신**: 유튜브 자막 요약 기능 수행 시 내부 FastAPI 서버에 요청
+ - **FastAPI → 외부 API 호출**: yt-dlp를 통해 자막 추출 기능 수행 후 Alan API 요약 결과 호출

### ▶ GitHub Actions

- `release` 브랜치 push 시:
  - 전체 Gradle 빌드 수행
  - `.jar` 및 설정 파일을 S3에 업로드
  - CodeDeploy 트리거 → EC2 배포 후 `start.sh` 실행
- AWS SSM Parameter Store에서 Jasypt 복호화 키(JASYPT_ENCRYPTOR_PASSWORD) 로드
- GitHub Secrets에는 별도 CI 환경변수만 관리

---

## 🔐 암호화 설정

- `application.properties` 내 민감한 키(`mail.password`, `oauth.client-secret` 등)는 Jasypt로 암호화
- 암호화 방법:
  ```bash
  java -jar jasypt-encryptor.jar input=비밀번호 key=secretary
  → ENC(암호문)
  ```
- 복호화:
  - EC2 서버 환경변수로 `JASYPT_ENCRYPTOR_PASSWORD` 설정
  - 복호화 키는 AWS SSM Parameter Store에 저장
- EC2 배포 시 start.sh 에서 JASYPT_ENCRYPTOR_PASSWORD 환경변수로 주입

---
