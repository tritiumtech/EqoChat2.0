CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    did VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100) UNIQUE,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500),
    password_hash VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    credit_score INT DEFAULT 50 CHECK (credit_score BETWEEN 0 AND 100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_did ON user_profile(did);
CREATE INDEX idx_user_phone ON user_profile(phone);
CREATE INDEX idx_user_status ON user_profile(status);

-- 初始数据
INSERT INTO user_profile (did, phone, nickname, status, credit_score) 
VALUES ('did:eqochat:system', '13800000000', 'System', 'ACTIVE', 100);