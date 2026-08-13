-- commerce-service : commerce_db (MariaDB)
-- 원칙: product_id 는 타 서비스 데이터이므로 FK 를 걸지 않고 값만 보관한다 (Database per Service).
--       주문 시점 상품명·단가는 스냅샷으로 저장한다 (상품 정보가 나중에 바뀌어도 주문 내역 불변).

CREATE TABLE member (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  email      VARCHAR(120) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  name       VARCHAR(40)  NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_item (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id  BIGINT NOT NULL,
  product_id BIGINT NOT NULL,             -- 타 서비스 참조: FK 없음
  qty        INT    NOT NULL CHECK (qty > 0),
  added_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_cart UNIQUE (member_id, product_id),
  CONSTRAINT fk_cart_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE orders (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no       VARCHAR(20) NOT NULL UNIQUE,      -- 예: ZP1001
  member_id      BIGINT      NOT NULL,
  total_price    BIGINT      NOT NULL CHECK (total_price >= 0),
  status         VARCHAR(15) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','PAID','COMPLETED','CANCELLED')),
  payment_method VARCHAR(20),                       -- PAID 시점에 기록
  ordered_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at        TIMESTAMP,
  CONSTRAINT fk_order_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE order_item (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id     BIGINT       NOT NULL,
  product_id   BIGINT       NOT NULL,               -- 타 서비스 참조: FK 없음
  product_name VARCHAR(120) NOT NULL,               -- 스냅샷
  qty          INT          NOT NULL CHECK (qty > 0),
  unit_price   BIGINT       NOT NULL,               -- 스냅샷
  CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_orders_member ON orders(member_id, ordered_at);
