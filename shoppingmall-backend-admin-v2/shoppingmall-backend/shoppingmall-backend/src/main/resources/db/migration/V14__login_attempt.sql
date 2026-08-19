-- [3-2 조치] 로그인 실패 횟수 제한 / 계정 잠금
-- ⚠️ application.yml 이 ddl-auto: validate 이므로, 애플리케이션 기동 전에 반드시 먼저 적용할 것.
ALTER TABLE users
    ADD COLUMN login_fail_count INT      NOT NULL DEFAULT 0 COMMENT '연속 로그인 실패 횟수',
    ADD COLUMN locked_until     DATETIME NULL              COMMENT '계정 잠금 해제 시각',
    ADD COLUMN last_login_at    DATETIME NULL              COMMENT '마지막 로그인 성공 시각';

CREATE INDEX idx_users_locked_until ON users (locked_until);
