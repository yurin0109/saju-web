# Among the Stars · 수많은 별 속에서 나를 발견하는 시간

생년월일과 태어난 시간으로 **사주팔자를 직접 계산**하고, 오행 캐릭터(사슴·여우·곰·호랑이·수달)와 함께
나의 성향과 올해의 흐름을 읽어보는 사주 웹 서비스입니다.

> 운명을 맞히기보다, 나를 이해하는 시간으로.

## 배포 주소
**http://springweb-env.eba-tqb6d4rm.ap-northeast-2.elasticbeanstalk.com**

AWS Elastic Beanstalk (Java Corretto 17, 단일 인스턴스) + Amazon RDS (MySQL, 프리 티어) · 서울 리전

## 기간 / 인원
2026.10.15 ~ 2026.11.06 / 1인

## 기술 스택
| 구분 | 사용 기술 |
|---|---|
| 백엔드 | Java 17, Spring Boot 3, Spring Data JPA |
| 프론트 | HTML, CSS, JavaScript (단일 페이지 + 탭 구성) |
| DB | MySQL 8 (배포), H2 (로컬 실행) |
| 테스트 | JUnit 5 |
| 디자인 | Figma |

## 주요 기능 (탭 8개)
1. **홈** — 서비스 소개, 이용 순서
2. **사주 입력** — 이름, 생년월일 **직접 입력**(19940817 → 1994.08.17), **양력 / 음력 · 윤달**, 태어난 시간(모름 가능), 출생 지역, 성별, **요즘 고민**(6분류 + 직접 적기)
3. **내 결과** — 사주팔자 8글자, 오행 비율, 핵심 성향(관계 · 일 · 지칠 때), 강한/보완할 기운, **신살 · 귀인**(천을귀인 · 문창귀인 · 월덕귀인 · 역마살 · 도화살 · 화개살 · 양인살 · 괴강살과 그 풀이), **고민 맞춤 풀이 + 맞는 리포트 추천**, 성찰 질문
4. **올해 운세** — 올해 간지와 나의 일간 관계로 읽는 흐름 + 풀이 근거
5. **직장 · 학업** — 직장운(십성 분포로 읽는 일하는 스타일) / **학업운**(일간 오행 공부 스타일, 인성 · 관성으로 본 시험운)
6. **사랑운** — **연애운(애정운)**(연애 스타일 · 올해 인연의 흐름) / 결혼운(배우자 별 · 배우자 자리) / **궁합**(저장한 두 사람의 일간 · 일지 합충 · 오행 보완 → 점수) / **재회운**
7. **가족운** — 년주(어린 시절) · 월주(부모 · 형제) · 시주(자녀) 자리 풀이
8. **의견 보내기** — 화면 오른쪽 아래 💬 버튼으로 건의사항 남기기, 고민 풀이 아래 **"풀이가 안 맞아요"** 버튼(내 고민 · 받은 풀이 주제가 자동으로 채워짐 → 관리자 페이지 "고민 풀이" 필터로 모아 보고 단어 추가에 반영) → **관리자 페이지(/admin.html)** 에서 확인 · 처리 완료 표시, 회원 목록 · 잠금 해제, 회원 · 구매 · 매출 요약
9. **내 사주 목록** — 저장한 사주 조회 / 수정 / 삭제, **성찰 일기**

### 무료 기본 풀이 · 유료 심화 리포트
| 구분 | 내용 | 가격 |
|---|---|---|
| 무료 | 모든 탭의 기본 풀이 | 0원 |
| 유료 | 각 탭 아래 **심화 리포트** 8종 (올해 운세 · 직장운 · 학업운 · 연애운 · 결혼운 · 궁합 · 재회운 · 가족운) | 각 4,900원 (궁합은 상대 1명당) |
| 묶음 | 궁합을 뺀 심화 리포트 7종 한 번에 | 19,900원 |

- 심화 리포트는 기본 풀이보다 **3배 이상** 길다 (`ReportsTest` 가 글자 수로 검사)
  - 예) 올해 운세 심화: 총론 · **이번 달부터 12개월 월별 흐름**(매달 월주를 계산) · 영역별 · 오행 균형 · 행운 포인트 · 분기별 플랜
- 고민 분류에 따라 내 결과에서 맞는 심화 리포트를 추천 (연애 → 결혼운, 일 → 직장운, 관계 → 궁합 …)
- 잠금은 **서버에서** 막음: `/api/profiles/{id}/premium/{code}` 는 구매하지 않았으면 403
- 구매는 사주 프로필마다 따로. **궁합 심화는 상대 1명마다** 따로 구매 (묶음에는 궁합이 없음 → 한 번 결제로 여러 사람과 보는 것 방지). 결제와 연결된 사주는 생년월일 · 시간 · 성별 수정 불가. 구매 취소 없음. 구매 내역은 화면 맨 아래 "구매 내역" 링크

### 결제 (토스페이먼츠 테스트 결제창)
```
[화면] 결제하기 → POST /api/payments/orders   서버가 주문번호 · 금액을 정해 저장
      → 토스 결제창 (테스트 카드 결제)
      → successUrl 로 돌아옴 (?paymentKey&orderId&amount)
      → POST /api/payments/confirm            서버가 저장한 금액과 비교 → 토스 승인 API → 구매 저장
```
- 금액은 화면이 아니라 **서버가 정한 값**으로만 승인 (금액 조작 방지)
- 시크릿 키는 서버에만 있고 화면으로 보내지 않음
- 키가 설정되지 않으면 **모의 결제**로 자동 전환. 토스 모드에서는 모의 결제 주소(`POST /api/purchases`)가 막힘

**토스 테스트 키 넣는 법**
1. [토스페이먼츠 개발자센터](https://developers.tosspayments.com) 회원가입 → 내 개발정보 → **API 개별 연동 키**의 테스트 클라이언트 키(`test_ck_…`) · 시크릿 키(`test_sk_…`) 복사
2. `saju-web` 폴더 바로 안(pom.xml 옆)에 `toss.properties` 파일을 만들고 두 줄 적기 → 평소처럼 실행
   ```properties
   toss.client-key=test_ck_...
   toss.secret-key=test_sk_...
   ```
   (`toss.properties` 는 .gitignore 에 들어 있어서 GitHub 에 올라가지 않음. 배포 서버에서는 환경변수 TOSS_CLIENT_KEY · TOSS_SECRET_KEY 사용)
3. 테스트 결제라 실제로 돈이 나가지 않음. 실제 결제는 가맹점 계약 필요

### 음력
- `LunarCalendar` — 1900 ~ 2050년 음력 ↔ 양력 변환
- 표는 천문 계산(Meeus 알고리즘: 합삭 · 중기 시각)을 **한국 표준시(UTC+9)** 로 날짜를 정해 만듦
  → 중국 음력과 다른 해도 한국 기준 (1997 · 2027년 설날, 2012년 윤3월, 2017년 윤5월)
- 테스트: 1900 ~ 2050년 매일 양력 → 음력 → 양력 왕복 검사

### 성찰 질문 · 성찰 일기
- 질문 데이터 **610개** (`src/main/resources/reflection-questions.txt`)
  - 일간 오행별 60개 × 5 + 공통 60개 → 내 결과 탭
  - 올해와의 관계별 30개 × 5 → 올해 운세 탭
  - 직장 50개 → 직장운 탭 / 관계 50개 → 결혼운 탭
- 날짜와 프로필로 그날의 질문이 정해지고, "다른 질문"으로 다음 질문을 볼 수 있음
- 저장할 때 질문 문구는 화면에서 보낸 값이 아니라 서버의 질문 데이터에서 다시 찾아 저장 (조작 방지)
- 프로필을 삭제하면 그 프로필로 쓴 성찰 일기 · 구매 내역도 함께 삭제

## 구조
```
[브라우저 index.html]
   │  fetch (X-Client-Id 헤더)
   ▼
[ProfileController]  [ReflectionController]  [PurchaseController]   REST API
   ▼                          ▼
[ProfileService] ◄──────── [ReflectionService] ──► [QuestionBank]  성찰 질문 610개
   │                          └─► [PurchaseService]  구매 여부 확인 (유료 잠금)
   │  └─► [SajuCalculator]  팔자 · 오행 · 올해 관계 계산 (직접 구현)
   │  └─► [Interpretation]  조건별 풀이 문구 템플릿
   ▼                          ▼
[ProfileRepository]        [ReflectionRepository] ──► DB (profile, reflection, purchase 테이블)
```
- **AI API 없이** 계산과 풀이 로직을 전부 직접 구현했습니다.
- 사주 결과는 DB에 저장하지 않고 조회할 때마다 계산합니다. (계산이 가볍고, 로직을 고치면 바로 반영)
- 로그인 대신 브라우저마다 발급한 고유 ID(UUID)로 "내 사주"를 구분합니다.

## API
| 메서드 | 주소 | 설명 |
|---|---|---|
| GET | /api/profiles | 내 사주 목록 |
| GET | /api/profiles/{id} | 사주 결과 |
| POST | /api/profiles | 사주 등록 |
| PUT | /api/profiles/{id} | 정보 수정 (사주 재계산) |
| DELETE | /api/profiles/{id} | 삭제 (성찰 일기 · 구매 내역 함께 삭제) |
| GET | /api/reflections/question?profileId=&type=core\|year\|career\|marriage&offset= | 오늘의 성찰 질문 |
| GET | /api/reflections?profileId= | 성찰 일기 목록 |
| POST | /api/reflections | 성찰 일기 저장 |
| PUT | /api/reflections/{id} | 답 수정 |
| DELETE | /api/reflections/{id} | 성찰 일기 삭제 |
| GET | /api/profiles/{id}/compat?partnerId= | 궁합 기본 풀이 (무료) |
| GET | /api/profiles/{id}/premium/{code}?partnerId= | 심화 리포트 (구매한 경우만, 아니면 403) |
| GET | /api/products | 상품 목록 (심화 리포트 8종 + 7종 묶음) |
| GET | /api/purchases | 내 구매 내역 (로그인했을 때만) |
| POST | /api/purchases | 모의 결제 (토스 키가 없을 때만, 로그인 필수) |
| GET | /api/auth/me | 로그인 상태 |
| POST | /api/auth/signup | 회원가입 (가입 후 바로 로그인, 이 브라우저의 사주를 계정으로 옮김) |
| POST | /api/auth/login | 로그인 (비밀번호 5번 틀리면 10분 잠금) |
| POST | /api/auth/logout | 로그아웃 |
| POST | /api/auth/find-id | 아이디 찾기 (복구 코드) |
| POST | /api/auth/reset-password | 비밀번호 재설정 (아이디 + 복구 코드 → 새 복구 코드 발급) |
| POST | /api/auth/change-password | 비밀번호 변경 (로그인, 지금 비밀번호 확인) |
| POST | /api/auth/recovery-code | 복구 코드 새로 받기 (로그인, 비밀번호 확인) |
| POST | /api/feedback | 건의사항 보내기 (로그인 없이 가능, 하루 10개까지) |
| GET | /api/admin/summary · /feedback · /members | 관리자 (X-Admin-Key = ADMIN_PASSWORD) |
| POST | /api/purchases/{id}/refund | 환불 (로그인, 구매 후 7일 이내 · 열어보지 않은 리포트, 토스 결제 취소 API) |
| GET | /api/payments/config | 결제 방식 (toss / mock) + 클라이언트 키 |
| POST | /api/payments/orders | 토스 주문 생성 (서버가 금액 결정, 로그인 필수) |
| POST | /api/payments/confirm | 토스 결제 승인 → 구매 저장 (로그인 필수) |

## 핵심 구현: 사주 계산 정책
| 항목 | 정책 |
|---|---|
| 년주 | 1월 1일이 아니라 **입춘(2/4)** 기준으로 해가 바뀜 |
| 월주 | 음력 1일이 아니라 **12절기** 기준으로 달이 바뀜 |
| 일주 | 2000-01-01(무오일) 기준 날짜 차이 ÷ 60의 나머지 |
| 시주 | 한국 표준시 30분 보정 → 자시 23:30~01:29. 23:30 이후 출생은 다음 날 일주 |
| 오행 비율 | 태어난 계절을 나타내는 **월지에 가중치 3**, 나머지 글자 1. 합계 100% 보정 |

**알려진 한계:** 절기 날짜를 평균값으로 고정해서, 절기 당일 전후 출생자는 ±1일 오차가 날 수 있습니다.
(개선 계획: 한국천문연구원 절기 API 연동) 음력은 1900 ~ 2050년만 지원합니다.

## 테스트
`SajuCalculatorTest` — 경계 케이스 18개
- 입춘 전날/당일 년주, 1월 1일 출생
- 절기(한로) 전날/당일 월주, 1월 초 소한 전후
- 기준일 이전 날짜 일주, 60일 주기
- 13:29 / 13:30 시지 경계, 23:29 / 23:30 일주 변경
- 시간 모름 처리, 오행 비율 합계 100, 월지 가중치

`TenGodsTest` — 십성 6개
- 7글자 십성 분포(월지 2배), 시간 모름 처리, 배우자 별 · 배우자 자리, 점수 구간, 문구 누락 검사

`QuestionBankTest` — 성찰 질문 7개
- 600개 이상, 추가해도 기존 질문 코드 유지, 분류 11개 모두 존재, 문구 중복 없음
- 코드로 찾기, 같은 날 같은 질문 · 다른 질문 보기 동작, 탭별로 맞는 분류에서만 나오는지

`ProductTest` — 유료 상품 4개
- 묶음이 궁합을 뺀 7종을 모두 여는지, 구매 내역 → 열린 리포트 계산, 이미 가진 리포트 재구매 차단, 묶음 30% 이상 할인

`LunarCalendarTest` — 음력 6개
- 설날 변환, 한국 · 중국이 다른 해, 윤달, 1900 ~ 2050년 매일 왕복, 한 해 일수, 없는 날짜 거절

`ReportsTest` — 리포트 5개
- 심화 리포트가 무료 풀이의 3배 이상인지(올해 운세 · 직장운 · 결혼운), 여러 생일로 빈 문구가 없는지, 이번 달부터 12개월, 궁합 합 · 충 · 천간합, 고민 없음 처리

## 보안

| 항목 | 내용 |
| --- | --- |
| 결제 우회 방지 | 유료 리포트는 서버에서 구매 여부를 확인한 뒤에만 내려준다 (미구매 403). 결제 금액은 서버가 정하고, 토스 승인 때 금액이 다르면 거절 |
| 궁합 재사용 방지 | 궁합 심화는 상대 1명 = 구매 1번. 묶음에도 궁합은 없음. 결제와 연결된 사주는 생년월일 · 시간 · 성별 수정 불가 (생일만 바꿔 다른 사람 것을 보는 것 방지) |
| 보안 헤더 | CSP(허용한 출처만 스크립트 · 폰트 · 결제창), X-Frame-Options DENY(클릭재킹 방지), nosniff, Referrer-Policy, API 응답 no-store |
| 요청 횟수 제한 | IP마다 1분에 조회 1200번 · 저장/결제 120번. 넘으면 429 |
| 입력 검증 | 모든 입력에 길이 · 형식 검사 (Bean Validation), 결제 키 · 주문번호는 영문 · 숫자만 |
| 로그인 | 아이디 · 비밀번호. 비밀번호는 PBKDF2-SHA256(소금 + 12만 번 반복) 해시만 저장. 5번 틀리면 10분 잠금, 로그인 · 가입은 IP당 1분 20번 |
| 세션 | 서버 세션 + HttpOnly · SameSite=Lax 쿠키(ATS_SESSION). 로그인할 때 세션 번호를 새로 발급(세션 고정 공격 방지) |
| 로그인 범위 | 무료 풀이는 로그인 없이, 결제 · 구매 내역은 로그인 필수. 로그인하면 브라우저 ID로 저장한 사주 · 성찰 일기를 계정으로 옮김 |
| 데이터 분리 | 로그인 전: X-Client-Id(UUID 형식 검사), 로그인 후: 세션의 계정 열쇠로 내 데이터만 접근 (헤더를 바꿔 보내도 세션 값이 우선) |
| XSS | 화면에 그리는 사용자 입력은 모두 escape |
| 에러 숨김 | 예상 못한 에러는 서버 로그에만 남기고, 화면에는 일반 문구만 (예외 이름 · 스택 노출 없음) |
| 저장 개수 제한 | 사용자당 사주 30개까지 |
| 계정 복구 | 이메일 대신 가입 때 한 번 보여주는 16자리 복구 코드(80비트). DB에는 SHA-256만 저장, 쓰면 새 코드로 교체 |
| CSRF | 바꾸는 요청은 Sec-Fetch-Site · Origin 이 우리 사이트일 때만, 본문은 JSON 만, 64KB 제한. 세션 쿠키 SameSite=Lax |
| CSP | 화면 스크립트를 app.js 파일로 분리해 인라인 스크립트 실행 금지 (script-src 'self' + 토스만) |
| 계정 열쇠 보호 | 회원 데이터 열쇠(ownerKey)는 화면에 절대 내려주지 않음. 로그인 없이 그 값으로 요청하면 401. 토스에는 SHA-256 으로 바꾼 고객 키만 보냄 |
| 환불 | 구매 후 7일 이내 · 미열람만 (전자상거래법 17조 2항 디지털 콘텐츠 예외). 리포트를 열면 opened_at 기록 |
| 관리자 | /admin.html, 비밀번호는 환경 변수 ADMIN_PASSWORD 로만 (코드에 없음, 8자 이상). 10번 틀리면 15분 잠금, 탭을 닫으면 비밀번호 사라짐 |
| 비밀 정보 | 토스 시크릿 키 · DB 비밀번호는 코드에 없음. toss.properties(깃 제외)나 환경 변수로만 |

## 실행 방법
```bash
# 로컬 실행 (H2 DB, 설치 필요 없음)
mvn spring-boot:run        # 또는 IntelliJ에서 SajuApplication 실행
# → http://localhost:8080

# 테스트
mvn test
```

## 배포 (Render + Neon PostgreSQL)
1. [Neon](https://neon.tech)에서 무료 PostgreSQL DB를 만들고 접속 정보를 확인
2. Render › Web Service › Environment 에 환경변수 등록 (코드에 비밀번호를 쓰지 않음)
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://호스트/neondb?sslmode=require
DB_USERNAME=...
DB_PASSWORD=...
```
3. 저장하면 자동으로 다시 배포되고, 이후 서버가 재시작돼도 데이터가 유지됨
4. Health Check Path: `/api/version`

MySQL을 쓸 때는 `SPRING_PROFILES_ACTIVE=mysql` 과 `jdbc:mysql://...` 주소를 사용합니다. 테이블 구조는 `db/schema.sql` 참고.

## 배포 (AWS EC2 + Docker)
1. AWS 콘솔 → EC2 → 인스턴스 시작: Ubuntu 24.04, **t3.micro**(프리 티어), 키 페어 생성(.pem 다운로드)
2. 보안 그룹 인바운드: SSH 22 (내 IP), HTTP 80 (0.0.0.0/0)
3. 접속: `ssh -i 키.pem ubuntu@퍼블릭IP`
4. 서버에서
   ```bash
   sudo apt update && sudo apt install -y docker.io git
   git clone https://github.com/<내 아이디>/<저장소>.git && cd <저장소>
   sudo docker build -t saju .
   sudo docker run -d --name saju --restart always -p 80:8080 \
     -v /home/ubuntu/saju-data:/app/data \
     -e TOSS_CLIENT_KEY=test_ck_... -e TOSS_SECRET_KEY=test_sk_... saju
   ```
5. 브라우저에서 `http://퍼블릭IP` 접속. IP가 바뀌지 않게 하려면 **탄력적 IP** 연결
- `-v` 로 H2 DB 파일을 서버 디스크에 두어서 컨테이너를 다시 만들어도 데이터 유지 (RDS MySQL 을 쓰면 `prod` 프로필 + DB_URL 환경변수)
- 코드 수정 후 반영: `git pull && sudo docker build -t saju . && sudo docker rm -f saju && (위 docker run 다시)`
- 발표 끝나면 인스턴스 **중지/종료** (프리 티어 시간 초과 시 요금)

## 캐릭터 이미지
오행 캐릭터 5종은 `src/main/resources/static/img/` 의 SVG 파일입니다.
`wood.svg`(사슴) · `fire.svg`(여우) · `earth.svg`(곰) · `metal.svg`(호랑이) · `water.svg`(수달)
같은 이름의 파일로 바꾸면 화면 전체에 반영됩니다.

## 트러블슈팅
<!-- 개발하면서 막혔던 문제 → 원인 → 해결을 여기에 기록하세요 -->
- 
