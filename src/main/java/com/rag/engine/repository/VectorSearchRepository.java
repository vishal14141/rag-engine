
package com.rag.engine.repository;

import com.rag.engine.dto.ChunkParagraph;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import com.rag.engine.util.FinancialYearTitleFilter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class VectorSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ParagraphRepository paragraphRepository;

    public VectorSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ParagraphRepository paragraphRepository) {

        this.jdbcTemplate = jdbcTemplate;
        this.paragraphRepository = paragraphRepository;
    }

    public List<RankedChunk> search(
            float[] queryEmbedding,
            int limit,
            List<String> financialYears) {

        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("Query embedding cannot be empty");
        }

        List<String> documentTitles =
                FinancialYearTitleFilter.toDocumentTitles(financialYears);

        String vector = Arrays.toString(queryEmbedding);

        String titleFilter = documentTitles.isEmpty()
                ? ""
                : " AND doc.document_title IN (:documentTitles) ";

        String sql = """
                SELECT
                    dc.id,
                    dc.section,
                    dc.content,
                    dc.start_page,
                    dc.end_page,
                    dc.embedding <=> CAST(:vector AS vector) AS distance
                FROM document_chunks dc
                JOIN documents doc
                    ON doc.id = dc.document_id
                WHERE 1 = 1
                %s
                ORDER BY distance ASC
                LIMIT :limit
                """.formatted(titleFilter);

        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("vector", vector)
                        .addValue("limit", limit);

        if (!documentTitles.isEmpty()) {
            parameters.addValue("documentTitles", documentTitles);
        }

        List<RetrievedChunk> chunks = jdbcTemplate.query(
                sql,
                parameters,
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

        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<ChunkParagraph>> paragraphs =
                paragraphRepository.findByChunkIds(
                        chunks.stream()
                                .map(RetrievedChunk::id)
                                .toList()
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

    // Backward compatibility for existing callers.
    public List<RankedChunk> search(
            float[] queryEmbedding,
            int limit) {

        return search(queryEmbedding, limit, null);
    }
}
