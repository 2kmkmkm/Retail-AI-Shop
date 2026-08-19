-- 가상 POS 단말이 남기는 오프라인 판매 로그
-- Debezium이 이 테이블의 binlog를 캡처해 pos.pos_db.pos_stock_logs 토픽으로 발행한다.
CREATE TABLE IF NOT EXISTS pos_stock_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id VARCHAR(50) NOT NULL,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    category VARCHAR(100),
    changed_qty INT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
