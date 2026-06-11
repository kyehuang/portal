CREATE TABLE users (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE user_sections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    section_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sections_user_id_section_id (user_id, section_id),
    INDEX idx_user_sections_section_id (section_id),
    CONSTRAINT fk_user_sections_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE ars (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    section VARCHAR(100) NOT NULL,
    priority VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    creator VARCHAR(100) NOT NULL,
    assignee VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    due_date TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_ars_priority CHECK (priority IN ('p0', 'p1', 'p2')),
    CONSTRAINT chk_ars_status CHECK (status IN ('in_progress', 'done'))
);

CREATE TABLE ar_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ar_id VARCHAR(36) NOT NULL,
    tag VARCHAR(255) NOT NULL,
    normalized_tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ar_tags_ar_id_normalized_tag (ar_id, normalized_tag),
    INDEX idx_ar_tags_normalized_tag (normalized_tag),
    CONSTRAINT fk_ar_tags_ar_id FOREIGN KEY (ar_id) REFERENCES ars (id) ON DELETE CASCADE
);

CREATE TABLE sub_ars (
    id VARCHAR(36) NOT NULL,
    parent_ar_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    section VARCHAR(100) NOT NULL,
    priority VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    creator VARCHAR(100) NOT NULL,
    assignee VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    due_date TIMESTAMP NULL,
    PRIMARY KEY (id),
    INDEX idx_sub_ars_parent_ar_id (parent_ar_id),
    CONSTRAINT fk_sub_ars_parent_ar_id FOREIGN KEY (parent_ar_id) REFERENCES ars (id) ON DELETE CASCADE,
    CONSTRAINT chk_sub_ars_priority CHECK (priority IN ('p0', 'p1', 'p2')),
    CONSTRAINT chk_sub_ars_status CHECK (status IN ('in_progress', 'done'))
);

CREATE TABLE sub_ar_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sub_ar_id VARCHAR(36) NOT NULL,
    tag VARCHAR(255) NOT NULL,
    normalized_tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sub_ar_tags_sub_ar_id_normalized_tag (sub_ar_id, normalized_tag),
    INDEX idx_sub_ar_tags_normalized_tag (normalized_tag),
    CONSTRAINT fk_sub_ar_tags_sub_ar_id FOREIGN KEY (sub_ar_id) REFERENCES sub_ars (id) ON DELETE CASCADE
);
