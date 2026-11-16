package com.rag.engine.dto;

public record ChunkParagraph(
    String paragraphNumber,
    String content,
    int pageNumber
){}
