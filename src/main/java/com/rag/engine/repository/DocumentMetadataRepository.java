package com.rag.engine.repository;

public interface DocumentMetadataRepository {

    boolean existsByFileHash(String fileHash);
    boolean existsByDocumentTitle(String documentTitle);
}
