CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NOT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    details VARCHAR(1024),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_operation_log_operator FOREIGN KEY (operator_id) REFERENCES users (id)
);

INSERT INTO roles (role_name, role_code, description)
SELECT 'System Administrator', 'ADMIN', 'System management administrator'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE role_code = 'ADMIN'
);

INSERT INTO users (username, password, real_name, department, status, created_at, updated_at)
SELECT 'admin', '$2a$10$abcdefghijklmnopqrstuv', '系统管理员', '系统管理', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
    AND NOT EXISTS (
        SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);
