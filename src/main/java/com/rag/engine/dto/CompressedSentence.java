package com.rag.engine.dto;

import java.util.UUID;

public record CompressedSentence(
        UUID chunkId,
        String section,
        String paragraphNumber,
        int pageNumber,
        String content,
        double similarityScore
) {}
