CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(64) NOT NULL UNIQUE,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64),
    department VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE approval_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_id BIGINT NOT NULL,
    approver_id BIGINT,
    business_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_approval_flow_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT fk_approval_flow_approver FOREIGN KEY (approver_id) REFERENCES users (id)
);

CREATE TABLE approval_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    approval_result VARCHAR(16) NOT NULL,
    comment VARCHAR(512),
    approval_time DATETIME,
    CONSTRAINT fk_approval_node_flow FOREIGN KEY (flow_id) REFERENCES approval_flow (id),
    CONSTRAINT fk_approval_node_approver FOREIGN KEY (approver_id) REFERENCES users (id)
);

CREATE TABLE procurement_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    applicant_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_amount DECIMAL(18, 2),
    status VARCHAR(32) NOT NULL,
    approval_flow_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_procurement_application_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT fk_procurement_application_flow FOREIGN KEY (approval_flow_id) REFERENCES approval_flow (id)
);

CREATE TABLE procurement_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    expected_price DECIMAL(18, 2),
    CONSTRAINT fk_procurement_item_application FOREIGN KEY (application_id) REFERENCES procurement_application (id)
);

CREATE TABLE inventory_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(8) NOT NULL,
    applicant_id BIGINT NOT NULL,
    reason VARCHAR(256),
    status VARCHAR(32) NOT NULL,
    approval_flow_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_inventory_application_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT fk_inventory_application_flow FOREIGN KEY (approval_flow_id) REFERENCES approval_flow (id)
);

CREATE TABLE inventory_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    actual_quantity INT,
    CONSTRAINT fk_inventory_item_application FOREIGN KEY (application_id) REFERENCES inventory_application (id)
);

CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL UNIQUE,
    current_stock INT NOT NULL,
    safety_stock INT NOT NULL,
    locked_stock INT NOT NULL
);

CREATE TABLE inventory_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_date DATE NOT NULL,
    total_value DECIMAL(18, 2),
    turnover_rate DECIMAL(10, 4),
    obsolete_count INT,
    low_stock_count INT
);

CREATE TABLE operation_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_date DATE NOT NULL,
    in_count INT,
    out_count INT,
    approval_count INT
);

INSERT INTO roles (role_name, role_code, description)
VALUES ('Purchaser', 'PURCHASER', 'Procurement specialist'),
       ('Warehouse Operator', 'OPERATOR', 'Warehouse operations'),
       ('Warehouse Manager', 'MANAGER', 'Approval authority');

INSERT INTO users (username, password, real_name, department, status, created_at, updated_at)
VALUES ('purchaser1', '$2a$10$abcdefghijklmnopqrstuv', '采购员张三', '采购部', 'ACTIVE', NOW(), NOW()),
       ('operator1', '$2a$10$abcdefghijklmnopqrstuv', '仓管员李四', '仓储部', 'ACTIVE', NOW(), NOW()),
       ('manager1', '$2a$10$abcdefghijklmnopqrstuv', '仓库经理王五', '仓储部', 'ACTIVE', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON (u.username = 'purchaser1' AND r.role_code = 'PURCHASER')
   OR (u.username = 'operator1' AND r.role_code = 'OPERATOR')
   OR (u.username = 'manager1' AND r.role_code = 'MANAGER');
