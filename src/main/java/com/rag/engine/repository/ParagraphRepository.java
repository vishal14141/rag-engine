package com.rag.engine.repository;

import com.rag.engine.dto.ChunkParagraph;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ParagraphRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ParagraphRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<UUID, List<ChunkParagraph>> findByChunkIds(
            Collection<UUID> chunkIds
    ) {

        if (chunkIds == null || chunkIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT
                    chunk_id,
                    paragraph_number,
                    content,
                    page_number
                FROM document_paragraphs
                WHERE chunk_id IN (:chunkIds)
                ORDER BY chunk_id, sequence_number
                """;

        Map<UUID, List<ChunkParagraph>> result = new HashMap<>();

        jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("chunkIds", chunkIds),
                rs -> {
                    UUID chunkId = rs.getObject("chunk_id", UUID.class);

                    ChunkParagraph paragraph = new ChunkParagraph(
                            rs.getString("paragraph_number"),
                            rs.getString("content"),
                            rs.getInt("page_number")
                    );

                    result.computeIfAbsent(
                            chunkId,
                            ignored -> new ArrayList<>()
                    ).add(paragraph);
                }
        );

        return result;
    }
}

