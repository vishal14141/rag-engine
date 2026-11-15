package com.rag.engine.repository;

import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class KeywordSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KeywordSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RankedChunk> search(String query, int limit) {

        String sql = """
                WITH search_lexemes AS (
                    SELECT lexeme
                    FROM unnest(
                        tsvector_to_array(
                            to_tsvector('english', :query)
                        )
                    ) AS lexeme
                    WHERE lexeme !~ '^[0-9]+$'
                      AND lexeme !~ '^-[0-9]+$'
                ),
                search_query AS (
                    SELECT string_agg(
                        quote_literal(lexeme),
                        ' | '
                    )::tsquery AS ts_query
                    FROM search_lexemes
                )
                SELECT
                    d.id,
                    d.section,
                    d.paragraph_numbers,
                    d.content,
                    d.start_page,
                    d.end_page,
                    ts_rank(
                        to_tsvector('english', d.content),
                        q.ts_query
                    ) AS keyword_score
                FROM document_chunks d
                CROSS JOIN search_query q
                WHERE q.ts_query IS NOT NULL
                  AND to_tsvector('english', d.content) @@ q.ts_query
                ORDER BY keyword_score DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                Map.of("query", query, "limit", limit),
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
                            rs.getDouble("keyword_score")
                    );
                }
        );
    }
}
