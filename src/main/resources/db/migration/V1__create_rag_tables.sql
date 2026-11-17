
-- Enable pgvector
CREATE EXTENSION IF NOT EXISTS vector;


-- Documents
CREATE TABLE documents (
    id UUID PRIMARY KEY,

    file_name VARCHAR(255) NOT NULL,
    document_title VARCHAR(500) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_documents_file_hash UNIQUE (file_hash)
);

-- Case-insensitive, whitespace-trimmed title uniqueness
CREATE UNIQUE INDEX uq_documents_title
    ON documents (LOWER(TRIM(document_title)));


-- Document chunks
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,

    document_id UUID NOT NULL
        REFERENCES documents(id) ON DELETE CASCADE,

    section VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,

    start_page INTEGER NOT NULL,
    end_page INTEGER NOT NULL,
    token_count INTEGER NOT NULL,

    embedding vector(1024) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_chunks_document_id
    ON document_chunks(document_id);


-- Individual paragraphs belonging to chunks
CREATE TABLE document_paragraphs (
    id UUID PRIMARY KEY,

    chunk_id UUID NOT NULL
        REFERENCES document_chunks(id) ON DELETE CASCADE,

    paragraph_number VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER NOT NULL,
    sequence_number INTEGER NOT NULL,

    CONSTRAINT uq_chunk_paragraph_sequence
        UNIQUE (chunk_id, sequence_number)
);

CREATE INDEX idx_document_paragraphs_chunk_id
    ON document_paragraphs(chunk_id);
