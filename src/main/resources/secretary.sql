drop table if exists ai_message;
drop table if exists ai_conversation;
drop table if exists schedule;
drop table if exists member;

CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    name VARCHAR(100),
    email VARCHAR(100),
    profile_image VARCHAR(255),
    UNIQUE KEY uk_provider_id (provider, provider_id)
);
CREATE TABLE ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE
);
CREATE TABLE ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender ENUM('USER', 'AI') NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE
);
CREATE TABLE schedule (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    start DATETIME NOT NULL,
    end DATETIME,
    location VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE
);
