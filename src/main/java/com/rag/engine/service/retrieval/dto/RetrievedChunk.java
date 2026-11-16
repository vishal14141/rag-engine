package com.rag.engine.service.retrieval.dto;

import com.rag.engine.dto.ChunkParagraph;

import java.util.List;
import java.util.UUID;

public record RetrievedChunk(

        UUID id,
        String section,
        List<ChunkParagraph> paragraphs,
        String content,
        int startPage,
        int endPage,
        double retrievalScore
) implements RankedChunk {
    @Override
    public RetrievedChunk chunk() {
        return this;
    }

    @Override
    public double score() {
        return this.retrievalScore;
    }
}
