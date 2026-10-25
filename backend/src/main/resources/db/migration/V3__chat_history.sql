-- Chat history: conversations, their messages, and the passages each answer cited
CREATE TABLE chat_conversation (
    id         UUID PRIMARY KEY,
    owner_id   UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    scope      VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_chat_conversation_owner ON chat_conversation (owner_id, updated_at DESC);

CREATE TABLE chat_message (
    id              UUID PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES chat_conversation (id) ON DELETE CASCADE,
    position        INT         NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_chat_message_conversation ON chat_message (conversation_id, position);

-- A copy of each cited passage, so old answers keep their citations after the documents change.
-- No foreign key to library_document: the document may be deleted later. When a private document is
-- deleted, its text is set to NULL here (title and article stay).
CREATE TABLE chat_message_source (
    id             UUID PRIMARY KEY,
    message_id     UUID             NOT NULL REFERENCES chat_message (id) ON DELETE CASCADE,
    position       INT              NOT NULL,
    document_id    UUID,
    document_title VARCHAR(300)     NOT NULL,
    scope          VARCHAR(20)      NOT NULL,
    article        VARCHAR(100),
    article_title  VARCHAR(300),
    chapter        VARCHAR(300),
    text           TEXT,
    score          DOUBLE PRECISION NOT NULL
);
CREATE INDEX idx_chat_message_source_message ON chat_message_source (message_id);
CREATE INDEX idx_chat_message_source_document ON chat_message_source (document_id);
