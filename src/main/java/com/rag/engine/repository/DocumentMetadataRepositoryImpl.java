package com.rag.engine.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentMetadataRepositoryImpl implements DocumentMetadataRepository{

    private final JdbcTemplate jdbcTemplate;

    public DocumentMetadataRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByFileHash(String fileHash) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM documents
                    WHERE file_hash = ?
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                fileHash
        );

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean existsByDocumentTitle(String documentTitle) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM documents
                    WHERE document_title = ?
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                documentTitle
        );

        return Boolean.TRUE.equals(exists);
    }
}
