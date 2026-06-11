-- V11: outbound mail log (audit + idempotency ledger)

SET NAMES utf8mb4;

CREATE TABLE email_log (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recipient  VARCHAR(255) NOT NULL,
    type       VARCHAR(40)  NOT NULL,
    order_id   BIGINT UNSIGNED DEFAULT NULL,
    subject    VARCHAR(255) NOT NULL,
    status     ENUM('SENT','FAILED') NOT NULL,
    error      VARCHAR(1000) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_email_log_order_type (order_id, type),
    CONSTRAINT fk_email_log_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
