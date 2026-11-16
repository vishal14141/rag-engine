package com.rag.engine.service.retrieval.fusion;

import com.rag.engine.config.RetrievalProperties;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class FusionOrchestrator {

    private final RetrievalProperties retrievalProperties;
    private final FusionStrategy strategy;


    public FusionOrchestrator(RetrievalProperties retrievalProperties, List<FusionStrategy> fusionStrategies) {
        this.retrievalProperties = retrievalProperties;
        this.strategy = fusionStrategies.stream().filter(strategy -> Objects.equals(retrievalProperties.getFusion().getStrategy(), strategy.getName())).findFirst().orElseThrow();
    }

    public List<RankedChunk> fuse(List<Map<String, List<RankedChunk>>> results) {

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        long rankingListCount = results.stream()
                .filter(Objects::nonNull)
                .mapToLong(Map::size)
                .sum();

        if (rankingListCount == 0) {
            return List.of();
        }

        log.info("Fusion started with {} independent ranking lists", rankingListCount);
        List<RankedChunk> rankedChunks = strategy.fuse(results, retrievalProperties.getFusion().getCandidateLimit());
        log.info("Fusion returned list of size: {}", rankedChunks.size());

        return rankedChunks;
    }

}
