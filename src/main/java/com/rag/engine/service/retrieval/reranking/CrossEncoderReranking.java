package com.rag.engine.service.retrieval.reranking;

import com.rag.engine.service.retrieval.dto.RankedChunk;

import java.util.List;

public class CrossEncoderReranking implements RerankingStrategy{
    @Override
    public String getName() {
        return "cross-encoder";
    }

    @Override
    public List<RankedChunk> rank(String query, List<RankedChunk> candidates, int limit) {
        return List.of();
    }
}
