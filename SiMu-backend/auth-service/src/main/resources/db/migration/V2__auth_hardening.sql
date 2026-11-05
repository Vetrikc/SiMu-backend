-- auth-service, PostgreSQL
-- Refresh tokens (rotation + reuse detection), e-mail tokens, login audit,
-- e-mail verification flag.

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- One row per issued refresh token. Only the SHA-256 hash is stored, a database leak
-- does not expose usable tokens.
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID,
    user_agent  VARCHAR(255),
    ip          VARCHAR(64),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user   ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

-- Verification and password-reset tokens.
CREATE TABLE email_tokens (
    id         UUID PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    type       VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_email_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_email_tokens_user ON email_tokens (user_id);

-- Who signed in, from where and with what result.
CREATE TABLE login_audit (
    id             UUID PRIMARY KEY,
    user_id        BIGINT,
    username       VARCHAR(255),
    success        BOOLEAN      NOT NULL,
    failure_reason VARCHAR(100),
    ip             VARCHAR(64),
    user_agent     VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_login_audit_user_created ON login_audit (user_id, created_at DESC);
CREATE INDEX ix_login_audit_created      ON login_audit (created_at DESC);
