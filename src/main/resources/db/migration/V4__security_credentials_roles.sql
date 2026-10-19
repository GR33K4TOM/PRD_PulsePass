CREATE TABLE user_credentials (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'STAFF', 'ADMIN')),
    PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

CREATE OR REPLACE FUNCTION link_legacy_user_credential(p_user_id BIGINT, p_password_hash VARCHAR)
RETURNS VOID AS $$
BEGIN
    INSERT INTO user_credentials (user_id, password_hash, password_changed_at)
    VALUES (p_user_id, p_password_hash, CURRENT_TIMESTAMP)
    ON CONFLICT (user_id) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        password_changed_at = CURRENT_TIMESTAMP;

    INSERT INTO user_roles (user_id, role)
    VALUES (p_user_id, 'USER')
    ON CONFLICT (user_id, role) DO NOTHING;
END;
$$ LANGUAGE plpgsql;
