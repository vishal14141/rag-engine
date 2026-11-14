package com.rag.engine.service.ingestion.document;

import com.rag.engine.dto.DocumentChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static com.rag.engine.constants.Constant.EMBEDDING_DIMENSION;

@Service
public class DocumentStorageService {

    private final JdbcTemplate jdbcTemplate;

    public DocumentStorageService(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID saveDocument(String fileName, List<DocumentChunk> chunks, List<float[]> embeddings) throws SQLException {


        validateInput(fileName, chunks, embeddings);

        UUID documentId = UUID.randomUUID();

        // 1. Save document metadata
        jdbcTemplate.update(
                "INSERT INTO documents (id, file_name) VALUES (?, ?)",
                documentId,
                fileName
        );

        // 2. Save chunks and embeddings in a batch
        String sql = """
                INSERT INTO document_chunks (
                    id,
                    document_id,
                    section,
                    content,
                    paragraph_numbers,
                    start_page,
                    end_page,
                    token_count,
                    embedding
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        List<java.sql.Array> sqlArrays = new ArrayList<>();

        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DocumentChunk chunk = chunks.get(i);
                    float[] embedding = embeddings.get(i);

                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, documentId);
                    ps.setString(3, chunk.getSection());
                    ps.setString(4, chunk.getContent());

                    java.sql.Array paragraphNumbers =
                            ps.getConnection().createArrayOf(
                                    "text",
                                    chunk.getParagraphNumbers()
                                            .toArray(String[]::new)
                            );

                    sqlArrays.add(paragraphNumbers);
                    ps.setArray(5, paragraphNumbers);

                    ps.setInt(6, chunk.getStartPage());
                    ps.setInt(7, chunk.getEndPage());
                    ps.setInt(8, chunk.getTokenCount());

                    PGobject vector = new PGobject();
                    vector.setType("vector");
                    vector.setValue(toVectorLiteral(embedding));

                    ps.setObject(9, vector);
                }

                @Override
                public int getBatchSize() {
                    return chunks.size();
                }
            });
        } finally {
            for (java.sql.Array array : sqlArrays) {
                array.free();
            }
        }

        return documentId;
    }

    private String toVectorLiteral(float[] embedding) {

        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                result.append(",");
            }

            result.append(embedding[i]);
        }

        return result.append("]").toString();
    }

    private void validateInput(
            String fileName,
            List<DocumentChunk> chunks,
            List<float[]> embeddings) {

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Chunks cannot be empty");
        }

        if (embeddings == null || chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException(
                    "Chunk count and embedding count must match"
            );
        }

        for (int i = 0; i < chunks.size(); i++) {

            float[] embedding = embeddings.get(i);

            if (embedding == null
                    || embedding.length != EMBEDDING_DIMENSION) {
                throw new IllegalArgumentException(
                        "Invalid embedding dimension at index " + i
                );
            }

            for (float value : embedding) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException(
                            "Invalid embedding value at index " + i
                    );
                }
            }
        }
    }

}
