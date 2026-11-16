package com.rag.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentChunk {

    private String section;
    private String content;
    private List<ChunkParagraph> paragraphs;
    private int startPage;
    private int endPage;
    private int tokenCount;

}
