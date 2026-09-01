-- 기존 데이터베이스를 위한 크레딧 기능 마이그레이션
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS credit DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS credit_amount DECIMAL(19,2) NULL;

-- 크레딧 충전 결제는 특정 Course에 속하지 않는다.
ALTER TABLE payments
    MODIFY COLUMN course_id BIGINT NULL;

CREATE TABLE IF NOT EXISTS user_credit_transactions (
    payment_id  BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    amount      DECIMAL(19,2)   NOT NULL,
    credited_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (payment_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
