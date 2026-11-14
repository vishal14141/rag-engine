package com.rag.engine.service.retrieval.fusion;

import com.rag.engine.config.RetrievalProperties;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public List<RankedChunk> fuse(Map<String, List<RankedChunk>> map) {

        if (map.isEmpty()) return List.of();

        if (map.size() == 1) {
            log.info("Only {} retrieved strategy returned result, skipping fusion", map.size());
            return new ArrayList<>(map.values().iterator().next());
        } else {
            log.info("Fusion started");
            List<RankedChunk> rankedChunks = strategy.fuse(map, retrievalProperties.getFusion().getCandidateLimit());
            log.info("Fusion returned list of size: {}", rankedChunks.size());
            return rankedChunks;
        }
    }

}
