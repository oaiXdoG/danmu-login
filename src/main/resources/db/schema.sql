CREATE TABLE login_user (
    id BIGSERIAL PRIMARY KEY,
    account VARCHAR(128),
    app_id VARCHAR(128) NOT NULL,
    anchor_open_id VARCHAR(256) NOT NULL,
    nickname VARCHAR(256) NOT NULL DEFAULT '',
    avatar_url TEXT NOT NULL DEFAULT '',
    login_ip VARCHAR(64) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_login_user_account UNIQUE (account),
    CONSTRAINT uk_login_user_app_anchor UNIQUE (app_id, anchor_open_id)
);

CREATE TABLE game_server (
    id SERIAL PRIMARY KEY,
    server_id INTEGER NOT NULL UNIQUE,
    server_name VARCHAR(128) NOT NULL,
    server_ip VARCHAR(255) NOT NULL,
    server_port INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_server_status ON game_server (status);

CREATE TABLE game_version (
    id SERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL UNIQUE,
    client_version VARCHAR(64) NOT NULL,
    resource_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
