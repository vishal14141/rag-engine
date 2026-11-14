package com.rag.engine.service.retrieval.dto;

public interface RankedChunk {

    RetrievedChunk chunk();
    double score();

}
