package com.rag.engine.service.retrieval.strategy;

import com.rag.engine.service.retrieval.dto.RankedChunk;

import java.util.List;

public interface RetrievalStrategy {

    String getName();

    List<RankedChunk> retrieve(String query, int candidateLimit);

}
