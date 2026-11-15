package com.rag.engine.service.retrieval.reranking;

import com.rag.engine.service.retrieval.dto.RankedChunk;

import java.util.List;

public interface RerankingStrategy {

    String getName();
    List<RankedChunk> rank(String query, List<RankedChunk> candidates, int limit);
}
