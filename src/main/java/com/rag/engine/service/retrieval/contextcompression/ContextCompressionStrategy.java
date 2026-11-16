package com.rag.engine.service.retrieval.contextcompression;

import com.rag.engine.dto.CompressedSentence;
import com.rag.engine.service.retrieval.dto.RankedChunk;

import java.util.List;

public interface ContextCompressionStrategy {

    String getName();
    List<CompressedSentence> compressContext(List<RankedChunk> chunks, String originalQuery);
}
