package com.rag.engine.service.retrieval.dto;


import java.util.Map;

public record FusedChunk(
        RetrievedChunk chunk,
        Map<String, Integer> strategyRanks,
        double score
) implements RankedChunk {


}
