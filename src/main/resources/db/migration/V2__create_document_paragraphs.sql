CREATE TABLE document_paragraphs (
    id UUID PRIMARY KEY,
    chunk_id UUID NOT NULL REFERENCES document_chunks(id)
        ON DELETE CASCADE,
    paragraph_number VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    page_number INT NOT NULL,
    sequence_number INT NOT NULL,

    CONSTRAINT uq_chunk_paragraph_sequence
        UNIQUE (chunk_id, sequence_number)
);

CREATE INDEX idx_document_paragraphs_chunk_id
    ON document_paragraphs(chunk_id);