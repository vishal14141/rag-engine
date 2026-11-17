package com.rag.engine.service.ingestion.document;

import com.rag.engine.dto.DocumentChunk;
import com.rag.engine.service.ingestion.dto.DocumentMetadata;

import java.sql.SQLException;
import java.util.List;

public interface DocumentStorageService {

    void saveDocument(DocumentMetadata metadata, List<DocumentChunk> chunks, List<float[]> embeddings) throws SQLException;
}
