package com.rag.engine.service.retrieval.fusion;

import com.rag.engine.service.retrieval.dto.RankedChunk;

import java.util.List;
import java.util.Map;

public interface FusionStrategy {

    String getName();
    List<RankedChunk> fuse(Map<String, List<RankedChunk>> map, int limit);
}
