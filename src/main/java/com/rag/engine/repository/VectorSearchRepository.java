package com.rag.engine.repository;

import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
public class VectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public VectorSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RankedChunk> search(float[] queryEmbedding, int limit) {

        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException(
                    "Query embedding cannot be empty"
            );
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

        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, vector);
                    ps.setString(2, vector);
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> {

                    var sqlArray = rs.getArray("paragraph_numbers");

                    List<String> paragraphs = sqlArray == null
                            ? List.of()
                            : Arrays.asList(
                            (String[]) sqlArray.getArray()
                    );

                    return new RetrievedChunk(
                            rs.getObject("id", UUID.class),
                            rs.getString("section"),
                            paragraphs,
                            rs.getString("content"),
                            rs.getInt("start_page"),
                            rs.getInt("end_page"),
                            rs.getDouble("distance")
                    );
                }
        );
    }
}
