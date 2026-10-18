CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE library_document (
    id                  UUID PRIMARY KEY,
    title               VARCHAR(300) NOT NULL,
    original_filename   VARCHAR(300) NOT NULL,
    content_type        VARCHAR(150),
    size_bytes          BIGINT       NOT NULL,
    sha256              VARCHAR(64)  NOT NULL,
    scope               VARCHAR(20)  NOT NULL,
    owner_id            UUID REFERENCES app_user (id) ON DELETE CASCADE,
    language            VARCHAR(5),
    status              VARCHAR(20)  NOT NULL,
    error_message       TEXT,
    chunk_count         INT          NOT NULL DEFAULT 0,
    requirements_status VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    created_at          TIMESTAMPTZ  NOT NULL,
    expires_at          TIMESTAMPTZ
);
CREATE INDEX idx_library_document_owner ON library_document (owner_id);
CREATE INDEX idx_library_document_sha ON library_document (sha256);

CREATE TABLE requirement (
    id          UUID PRIMARY KEY,
    document_id UUID         NOT NULL REFERENCES library_document (id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    article     VARCHAR(100),
    title       VARCHAR(300) NOT NULL,
    description TEXT         NOT NULL
);
CREATE INDEX idx_requirement_document ON requirement (document_id);

CREATE TABLE compliance_report (
    id                 UUID PRIMARY KEY,
    owner_id           UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    policy_document_id UUID        NOT NULL REFERENCES library_document (id) ON DELETE CASCADE,
    law_document_id    UUID        NOT NULL REFERENCES library_document (id) ON DELETE CASCADE,
    status             VARCHAR(20) NOT NULL,
    language           VARCHAR(5)  NOT NULL,
    total              INT         NOT NULL DEFAULT 0,
    processed          INT         NOT NULL DEFAULT 0,
    met                INT         NOT NULL DEFAULT 0,
    partial            INT         NOT NULL DEFAULT 0,
    missing            INT         NOT NULL DEFAULT 0,
    score              INT,
    error_message      TEXT,
    created_at         TIMESTAMPTZ NOT NULL,
    finished_at        TIMESTAMPTZ
);
CREATE INDEX idx_compliance_report_owner ON compliance_report (owner_id);

CREATE TABLE compliance_result (
    id          UUID PRIMARY KEY,
    report_id   UUID         NOT NULL REFERENCES compliance_report (id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    article     VARCHAR(100),
    title       VARCHAR(300) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL,
    explanation TEXT,
    evidence    TEXT
);
CREATE INDEX idx_compliance_result_report ON compliance_result (report_id);

-- Spring AI PGVector table (768 = gemini-embedding-001 with outputDimensionality 768)
CREATE TABLE vector_store (
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(768)
);
CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_vector_store_document ON vector_store ((metadata ->> 'documentId'));
