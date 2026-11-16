package com.rag.engine.repository;

import com.rag.engine.dto.ChunkParagraph;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class VectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ParagraphRepository paragraphRepository;

    public VectorSearchRepository(JdbcTemplate jdbcTemplate, ParagraphRepository paragraphRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.paragraphRepository = paragraphRepository;
    }

    public List<RankedChunk> search(float[] queryEmbedding, int limit) {

        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("Query embedding cannot be empty");
        }

        String vector = Arrays.toString(queryEmbedding);

        String sql = """
                SELECT
                    id,
                    section,
                    paragraph_numbers,
                    content,
                    start_page,
                    end_page,
                    embedding <=> CAST(? AS vector) AS distance
                FROM document_chunks
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

        List<RetrievedChunk> chunks = jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, vector);
                    ps.setString(2, vector);
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getObject("id", UUID.class),
                        rs.getString("section"),
                        List.of(),
                        rs.getString("content"),
                        rs.getInt("start_page"),
                        rs.getInt("end_page"),
                        rs.getDouble("distance")
                )
        );

        Map<UUID, List<ChunkParagraph>> paragraphs =
                paragraphRepository.findByChunkIds(
                        chunks.stream().map(RetrievedChunk::id).toList()
                );

        return chunks.stream()
                .map(chunk -> new RetrievedChunk(
                        chunk.id(),
                        chunk.section(),
                        paragraphs.getOrDefault(chunk.id(), List.of()),
                        chunk.content(),
                        chunk.startPage(),
                        chunk.endPage(),
                        chunk.retrievalScore()
                ))
                .map(chunk -> (RankedChunk) chunk)
                .toList();
    }
}
