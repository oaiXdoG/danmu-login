CREATE TABLE login_user (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(32) NOT NULL,
    account VARCHAR(128),
    app_id VARCHAR(128) NOT NULL,
    anchor_open_id VARCHAR(256) NOT NULL,
    nickname VARCHAR(256) NOT NULL DEFAULT '',
    avatar_url TEXT NOT NULL DEFAULT '',
    login_ip VARCHAR(64) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_login_user_account UNIQUE (account),
    CONSTRAINT uk_login_user_platform_app_anchor UNIQUE (platform, app_id, anchor_open_id)
);

CREATE TABLE game_version (
    id SERIAL PRIMARY KEY,
    app_id VARCHAR(128) NOT NULL UNIQUE,
    client_version VARCHAR(64) NOT NULL,
    resource_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
