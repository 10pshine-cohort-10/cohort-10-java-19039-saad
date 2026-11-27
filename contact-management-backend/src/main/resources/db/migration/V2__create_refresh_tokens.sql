-- Refresh tokens are opaque random strings; only their SHA-256 hash is stored.
CREATE TABLE refresh_tokens (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id    BIGINT       NOT NULL,
    token_hash NVARCHAR(64) NOT NULL,
    expires_at DATETIME2(3) NOT NULL,
    revoked    BIT          NOT NULL CONSTRAINT DF_refresh_tokens_revoked DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT DF_refresh_tokens_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT UQ_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT FK_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IX_refresh_tokens_user_id ON refresh_tokens (user_id);
