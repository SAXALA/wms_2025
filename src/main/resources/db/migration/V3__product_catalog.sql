CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    sku VARCHAR(64) NOT NULL UNIQUE,
    unit VARCHAR(32),
    price DECIMAL(18, 2)
);

INSERT INTO
    product (name, sku, unit, price)
VALUES (
        '工业轴承 A1',
        'SKU-1001',
        '箱',
        1250.00
    ),
    (
        '智能扫码枪',
        'SKU-1002',
        '台',
        980.00
    ),
    (
        '防静电手套',
        'SKU-1003',
        '包',
        65.00
    );

ALTER TABLE procurement_item
ADD CONSTRAINT fk_procurement_item_product FOREIGN KEY (product_id) REFERENCES product (id);

ALTER TABLE inventory_item
ADD CONSTRAINT fk_inventory_item_product FOREIGN KEY (product_id) REFERENCES product (id);

ALTER TABLE inventory
ADD CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (id);