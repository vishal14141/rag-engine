package com.rag.engine.service.retrieval.strategy;


import com.rag.engine.repository.VectorSearchRepository;
import com.rag.engine.service.embedding.EmbeddingService;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class VectorSearchStrategy implements RetrievalStrategy {

    private final VectorSearchRepository vectorSearchRepository;
    private final EmbeddingService embeddingService;

    public VectorSearchStrategy(VectorSearchRepository vectorSearchRepository, EmbeddingService embeddingService) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    public String getName() {
        return "vector";
    }

    @Override
    public List<RankedChunk> retrieve(String query, int limit) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        return vectorSearchRepository.search(queryEmbedding, limit);
    }

    @Override
    public List<RankedChunk> retrieve(String query, int limit, List<String> financialYears) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        return vectorSearchRepository.search(queryEmbedding, limit, financialYears);
    }
}
