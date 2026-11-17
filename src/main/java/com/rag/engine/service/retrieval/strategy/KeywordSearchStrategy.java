package com.rag.engine.service.retrieval.strategy;

import com.rag.engine.repository.KeywordSearchRepository;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KeywordSearchStrategy implements RetrievalStrategy {

    private final KeywordSearchRepository keywordSearchRepository;

    public KeywordSearchStrategy(KeywordSearchRepository keywordSearchRepository) {
        this.keywordSearchRepository = keywordSearchRepository;
    }

    @Override
    public String getName() {
        return "keyword";
    }

    @Override
    public List<RankedChunk> retrieve(String query, int limit) {
        return keywordSearchRepository.search(query, limit);
    }

    @Override
    public List<RankedChunk> retrieve(String query, int limit, List<String> financialYears) {
        return keywordSearchRepository.search(query, limit, financialYears);
    }
}
