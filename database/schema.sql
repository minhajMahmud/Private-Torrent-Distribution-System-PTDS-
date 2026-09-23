-- ============================================================
-- Private Torrent Distribution System - PostgreSQL Schema
-- Normalized to 3NF
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------- ROLES & USERS ----------
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE   -- ROLE_USER, ROLE_ADMIN
);

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username            VARCHAR(50)  NOT NULL UNIQUE,
    email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150),
    avatar_url          VARCHAR(500),
    is_email_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE verification_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    token_type  VARCHAR(30)  NOT NULL,        -- EMAIL_VERIFY, PASSWORD_RESET
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- CATEGORIES & TAGS ----------
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    parent_id   BIGINT REFERENCES categories(id)
);

CREATE TABLE tags (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE
);

-- ---------- FILES ----------
CREATE TABLE files (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    uploader_id     UUID NOT NULL REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    category_id     BIGINT REFERENCES categories(id),
    original_name   VARCHAR(255) NOT NULL,
    storage_key      VARCHAR(500) NOT NULL,       -- local path / MinIO object key
    size_bytes      BIGINT NOT NULL,
    mime_type       VARCHAR(150),
    checksum_sha256 VARCHAR(64),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    moderated_by    UUID REFERENCES users(id),
    moderated_at    TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    download_count  BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE file_tags (
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (file_id, tag_id)
);

-- ---------- TORRENTS (Phase 3) ----------
CREATE TABLE torrents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id         UUID NOT NULL UNIQUE REFERENCES files(id) ON DELETE CASCADE,
    info_hash       VARCHAR(40) NOT NULL UNIQUE,
    torrent_path    VARCHAR(500) NOT NULL,
    magnet_uri      TEXT NOT NULL,
    piece_length    INTEGER NOT NULL,
    tracker_urls    TEXT[] NOT NULL,
    seeders         INTEGER NOT NULL DEFAULT 0,
    leechers        INTEGER NOT NULL DEFAULT 0,
    completed       INTEGER NOT NULL DEFAULT 0,
    health_status   VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN', -- HEALTHY, LOW_SEEDS, DEAD
    last_scraped_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- ENGAGEMENT ----------
CREATE TABLE comments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id     UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id   UUID REFERENCES comments(id) ON DELETE CASCADE,
    content     VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ratings (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id     UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score       SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (file_id, user_id)
);

CREATE TABLE favorites (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_id     UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, file_id)
);

CREATE TABLE downloads (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id),
    file_id     UUID NOT NULL REFERENCES files(id),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(300),
    downloaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- NOTIFICATIONS & AUDIT ----------
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50) NOT NULL,     -- FILE_APPROVED, NEW_COMMENT, etc.
    title       VARCHAR(200) NOT NULL,
    message     VARCHAR(1000),
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,    -- USER_LOGIN, FILE_UPLOAD, FILE_APPROVE...
    entity_type VARCHAR(50),
    entity_id   VARCHAR(100),
    details     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- INDEXES ----------
CREATE INDEX idx_files_uploader ON files(uploader_id);
CREATE INDEX idx_files_category ON files(category_id);
CREATE INDEX idx_files_status ON files(status);
CREATE INDEX idx_comments_file ON comments(file_id);
CREATE INDEX idx_downloads_user ON downloads(user_id);
CREATE INDEX idx_downloads_file ON downloads(file_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);

-- ---------- SEED DATA ----------
INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');
