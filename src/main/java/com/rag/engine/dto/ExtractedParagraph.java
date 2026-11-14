package com.rag.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedParagraph {

    private String section;
    private String paragraphNumber;
    private String text;
    private int pageNumber;
}
