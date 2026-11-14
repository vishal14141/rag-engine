package com.rag.engine.service.retrieval.dto;

import java.util.List;
import java.util.UUID;

public record RetrievedChunk(

        UUID id,
        String section,
        List<String> paragraphNumbers,
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
