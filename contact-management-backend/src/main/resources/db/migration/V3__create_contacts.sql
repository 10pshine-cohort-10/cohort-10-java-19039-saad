-- Contacts table: scoped to a user.
CREATE TABLE contacts (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    user_id       BIGINT        NOT NULL,
    first_name    NVARCHAR(100) NOT NULL,
    last_name     NVARCHAR(100) NOT NULL,
    title         NVARCHAR(100) NULL,
    company       NVARCHAR(150) NULL,
    job_position  NVARCHAR(100) NULL,
    notes         NVARCHAR(MAX) NULL,
    avatar_url    NVARCHAR(500) NULL,
    website       NVARCHAR(255) NULL,
    address       NVARCHAR(255) NULL,
    city          NVARCHAR(100) NULL,
    state         NVARCHAR(100) NULL,
    country       NVARCHAR(100) NULL,
    postal_code   NVARCHAR(20)  NULL,
    birthday      DATE          NULL,
    created_at    DATETIME2(3)  NOT NULL CONSTRAINT DF_contacts_created_at DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2(3)  NOT NULL CONSTRAINT DF_contacts_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_contacts PRIMARY KEY (id),
    CONSTRAINT FK_contacts_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IX_contacts_user_id ON contacts (user_id);
CREATE INDEX IX_contacts_user_name ON contacts (user_id, last_name, first_name);
CREATE INDEX IX_contacts_user_company ON contacts (user_id, company);
CREATE INDEX IX_contacts_user_created_at ON contacts (user_id, created_at);

-- Contact emails: relational 1:N
CREATE TABLE contact_emails (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    contact_id BIGINT        NOT NULL,
    email      NVARCHAR(254) NOT NULL,
    label      NVARCHAR(30)  NOT NULL CONSTRAINT DF_contact_emails_label DEFAULT 'WORK',
    is_primary BIT           NOT NULL CONSTRAINT DF_contact_emails_is_primary DEFAULT 0,
    CONSTRAINT PK_contact_emails PRIMARY KEY (id),
    CONSTRAINT FK_contact_emails_contacts FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE,
    CONSTRAINT UQ_contact_emails_contact_email UNIQUE (contact_id, email)
);

CREATE INDEX IX_contact_emails_email ON contact_emails (email);
CREATE UNIQUE INDEX UQ_contact_emails_primary ON contact_emails (contact_id) WHERE is_primary = 1;

-- Contact phone numbers: relational 1:N
CREATE TABLE contact_phone_numbers (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    contact_id   BIGINT       NOT NULL,
    phone_number NVARCHAR(30) NOT NULL,
    label        NVARCHAR(30) NOT NULL CONSTRAINT DF_contact_phones_label DEFAULT 'WORK',
    is_primary   BIT          NOT NULL CONSTRAINT DF_contact_phones_is_primary DEFAULT 0,
    CONSTRAINT PK_contact_phone_numbers PRIMARY KEY (id),
    CONSTRAINT FK_contact_phone_numbers_contacts FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE
);

CREATE INDEX IX_contact_phones_phone ON contact_phone_numbers (phone_number);
CREATE UNIQUE INDEX UQ_contact_phone_numbers_primary ON contact_phone_numbers (contact_id) WHERE is_primary = 1;
