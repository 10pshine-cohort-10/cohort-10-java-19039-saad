-- Users: one row per registered account.
-- Email is stored lower-cased and phone in normalised form (+digits) by the service layer.
CREATE TABLE users (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    first_name    NVARCHAR(100) NOT NULL,
    last_name     NVARCHAR(100) NOT NULL,
    email         NVARCHAR(254) NOT NULL,
    phone         NVARCHAR(20)  NOT NULL,
    password_hash NVARCHAR(100) NOT NULL,
    token_version INT           NOT NULL CONSTRAINT DF_users_token_version DEFAULT 0,
    created_at    DATETIME2(3)  NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2(3)  NOT NULL CONSTRAINT DF_users_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT UQ_users_phone UNIQUE (phone)
);
