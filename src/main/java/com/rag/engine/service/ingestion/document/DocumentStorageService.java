
package com.rag.engine.service.ingestion.document;

import com.rag.engine.dto.ChunkParagraph;
import com.rag.engine.dto.DocumentChunk;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.rag.engine.constants.Constant.EMBEDDING_DIMENSION;

@Service
public class DocumentStorageService {

    private final JdbcTemplate jdbcTemplate;

    public DocumentStorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID saveDocument(
            String fileName,
            List<DocumentChunk> chunks,
            List<float[]> embeddings) throws SQLException {

        validateInput(fileName, chunks, embeddings);

        UUID documentId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO documents (id, file_name) VALUES (?, ?)",
                documentId,
                fileName
        );

        // Generate IDs before insertion so paragraph records can reference them.
        List<UUID> chunkIds = chunks.stream()
                .map(chunk -> UUID.randomUUID())
                .toList();

        saveChunks(documentId, chunks, embeddings, chunkIds);

        saveParagraphs(chunks, chunkIds);

        return documentId;
    }

    private void saveChunks(
            UUID documentId,
            List<DocumentChunk> chunks,
            List<float[]> embeddings,
            List<UUID> chunkIds) throws SQLException {

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
                public void setValues(PreparedStatement ps, int i)
                        throws SQLException {

                    DocumentChunk chunk = chunks.get(i);
                    float[] embedding = embeddings.get(i);

                    ps.setObject(1, chunkIds.get(i));
                    ps.setObject(2, documentId);
                    ps.setString(3, chunk.getSection());
                    ps.setString(4, chunk.getContent());

                    String[] paragraphNumbers = chunk.getParagraphs()
                            .stream()
                            .map(ChunkParagraph::paragraphNumber)
                            .distinct()
                            .toArray(String[]::new);

                    java.sql.Array paragraphArray =
                            ps.getConnection().createArrayOf(
                                    "text",
                                    paragraphNumbers
                            );

                    sqlArrays.add(paragraphArray);
                    ps.setArray(5, paragraphArray);

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
    }

    private void saveParagraphs(
            List<DocumentChunk> chunks,
            List<UUID> chunkIds) {

        String sql = """
                INSERT INTO document_paragraphs (
                    id,
                    chunk_id,
                    paragraph_number,
                    content,
                    page_number,
                    sequence_number
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        List<ParagraphRow> paragraphRows = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {

            DocumentChunk chunk = chunks.get(i);
            UUID chunkId = chunkIds.get(i);

            List<ChunkParagraph> paragraphs = chunk.getParagraphs();

            for (int j = 0; j < paragraphs.size(); j++) {

                ChunkParagraph paragraph = paragraphs.get(j);

                paragraphRows.add(
                        new ParagraphRow(
                                UUID.randomUUID(),
                                chunkId,
                                paragraph,
                                j + 1
                        )
                );
            }
        }

        if (paragraphRows.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i)
                    throws SQLException {

                ParagraphRow row = paragraphRows.get(i);

                ps.setObject(1, row.id());
                ps.setObject(2, row.chunkId());
                ps.setString(3, row.paragraph().paragraphNumber());
                ps.setString(4, row.paragraph().content());
                ps.setInt(5, row.paragraph().pageNumber());
                ps.setInt(6, row.sequenceNumber());
            }

            @Override
            public int getBatchSize() {
                return paragraphRows.size();
            }
        });
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

            DocumentChunk chunk = chunks.get(i);
            float[] embedding = embeddings.get(i);

            if (chunk == null || chunk.getParagraphs() == null
                    || chunk.getParagraphs().isEmpty()) {
                throw new IllegalArgumentException(
                        "Chunk paragraphs cannot be empty at index " + i
                );
            }

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

    private record ParagraphRow(
            UUID id,
            UUID chunkId,
            ChunkParagraph paragraph,
            int sequenceNumber
    ) {}
}
