-- auth-service, PostgreSQL
-- External identities (Google today, GitHub/Apple later) + nullable password for OAuth-only users.

ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

CREATE TABLE user_identities (
    id         UUID PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider   VARCHAR(30) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    -- an identity is unique per provider: this is what makes find-or-create race-safe
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, subject)
);

CREATE INDEX ix_user_identities_user ON user_identities (user_id);

-- auth_method makes it possible to answer "was this account always password-based?"
ALTER TABLE login_audit ADD COLUMN auth_method VARCHAR(20);
ALTER TABLE login_audit ADD COLUMN subject_hash VARCHAR(64);
