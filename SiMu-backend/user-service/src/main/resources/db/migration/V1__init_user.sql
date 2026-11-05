-- user-service, PostgreSQL
-- Local read model of the accounts (filled from Kafka) + idempotency table for consumed events.

CREATE TABLE user_profiles (
    id         UUID PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    username   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

-- one live profile per account and per username
CREATE UNIQUE INDEX ux_user_profiles_user_id_active ON user_profiles (user_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_user_profiles_username_active ON user_profiles (username) WHERE deleted_at IS NULL;
CREATE INDEX ix_user_profiles_username ON user_profiles (username);
CREATE INDEX ix_user_profiles_deleted_at ON user_profiles (deleted_at);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
