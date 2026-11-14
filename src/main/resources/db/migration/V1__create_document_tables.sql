CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id),

    section VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    paragraph_numbers TEXT[] NOT NULL,

    start_page INTEGER NOT NULL,
    end_page INTEGER NOT NULL,
    token_count INTEGER NOT NULL,

    embedding vector(3072) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_chunks_document_id
    ON document_chunks(document_id);