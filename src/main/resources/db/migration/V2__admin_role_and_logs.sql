CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    details VARCHAR(1024),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_operation_log_operator FOREIGN KEY (operator_id) REFERENCES users (id)
);

INSERT INTO roles (role_name, role_code, description)
VALUES ('System Administrator', 'ADMIN', 'System management administrator');

INSERT INTO users (username, password, real_name, department, status, created_at, updated_at)
VALUES ('admin', '$2a$10$abcdefghijklmnopqrstuv', '系统管理员', '系统管理', 'ACTIVE', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON u.username = 'admin' AND r.role_code = 'ADMIN';
