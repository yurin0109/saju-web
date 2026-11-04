-- ─────────────────────────────────────────────
-- 별의 결 · 사주 웹 DB 스키마 (MySQL 8)
-- JPA ddl-auto=update 로 자동 생성되지만,
-- 직접 만들거나 ERD 설명용으로 참고할 수 있게 남겨둔다.
-- ─────────────────────────────────────────────

CREATE DATABASE IF NOT EXISTS saju
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE saju;

CREATE TABLE IF NOT EXISTS profile (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '프로필 번호',
    client_id   VARCHAR(36)  NOT NULL                COMMENT '브라우저별 고유 ID (로그인 대체)',
    name        VARCHAR(20)  NOT NULL                COMMENT '이름',
    birth_date  DATE         NOT NULL                COMMENT '생년월일 (양력)',
    birth_time  TIME         NULL                    COMMENT '태어난 시간 (모르면 NULL)',
    gender      CHAR(1)      NOT NULL                COMMENT 'M / F',
    birth_place VARCHAR(30)  NULL                    COMMENT '출생지',
    calendar_type VARCHAR(5) NULL                    COMMENT 'SOLAR / LUNAR (NULL = 양력)',
    lunar_date  VARCHAR(10)  NULL                    COMMENT '음력으로 입력했을 때 입력한 음력 날짜 (birth_date 에는 변환한 양력)',
    leap_month  BIT          NULL                    COMMENT '윤달 여부',
    concern_type VARCHAR(10) NULL                    COMMENT '요즘 고민 분류: LOVE / CAREER / MONEY / RELATION / SELF / FUTURE',
    concern_text VARCHAR(200) NULL                   COMMENT '요즘 고민 (직접 입력)',
    created_at  DATETIME     NOT NULL                COMMENT '등록일시',
    PRIMARY KEY (id),
    INDEX idx_profile_client (client_id)
) ENGINE = InnoDB COMMENT = '사주 프로필';

-- 사주 결과(팔자, 오행, 풀이)는 저장하지 않고 조회할 때 계산한다.

CREATE TABLE IF NOT EXISTS reflection (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '성찰 일기 번호',
    client_id     VARCHAR(36)   NOT NULL                COMMENT '브라우저별 고유 ID',
    profile_id    BIGINT        NOT NULL                COMMENT '어떤 사주 프로필에 대한 기록인지',
    question_type VARCHAR(10)   NOT NULL                COMMENT 'core / year / career / marriage',
    question_code VARCHAR(30)   NOT NULL                COMMENT '질문 코드 (예: WOOD-1)',
    question_text VARCHAR(200)  NOT NULL                COMMENT '저장 당시 질문 문구',
    answer        VARCHAR(1000) NOT NULL                COMMENT '사용자가 적은 답',
    created_at    DATETIME      NOT NULL                COMMENT '작성일시',
    updated_at    DATETIME      NOT NULL                COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_reflection_client (client_id),
    INDEX idx_reflection_profile (profile_id)
) ENGINE = InnoDB COMMENT = '성찰 일기';
-- 프로필을 삭제하면 해당 profile_id 의 성찰 일기도 함께 삭제한다 (ProfileService.delete)

CREATE TABLE IF NOT EXISTS purchase (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '구매 번호',
    client_id    VARCHAR(36) NOT NULL                COMMENT '브라우저별 고유 ID',
    profile_id   BIGINT      NOT NULL                COMMENT '어떤 사주 프로필로 구매했는지',
    product_code VARCHAR(20) NOT NULL                COMMENT 'YEAR / CAREER / MARRIAGE / COMPAT / REUNION / FAMILY / BUNDLE',
    price        INT         NOT NULL                COMMENT '구매 당시 가격(원)',
    created_at   DATETIME    NOT NULL                COMMENT '구매일시',
    PRIMARY KEY (id),
    INDEX idx_purchase_client (client_id),
    INDEX idx_purchase_profile (profile_id)
) ENGINE = InnoDB COMMENT = '구매 내역';
-- 묶음(BUNDLE)을 사면 심화 리포트 6종이 모두 열린다 (Product.unlocks)

CREATE TABLE IF NOT EXISTS payment_order (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     VARCHAR(64)  NOT NULL UNIQUE      COMMENT '토스에 보내는 주문번호',
    client_id    VARCHAR(36)  NOT NULL,
    profile_id   BIGINT       NOT NULL,
    product_code VARCHAR(20)  NOT NULL,
    amount       INT          NOT NULL               COMMENT '서버가 정한 결제 금액 (승인 때 비교)',
    status       VARCHAR(10)  NOT NULL               COMMENT 'READY / PAID / FAILED',
    payment_key  VARCHAR(200) NULL,
    method       VARCHAR(30)  NULL,
    fail_reason  VARCHAR(300) NULL,
    purchase_id  BIGINT       NULL,
    created_at   DATETIME     NOT NULL,
    approved_at  DATETIME     NULL,
    PRIMARY KEY (id),
    INDEX idx_order_client (client_id)
) ENGINE = InnoDB COMMENT = '토스 결제 주문';
