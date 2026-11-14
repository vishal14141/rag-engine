package com.rag.engine.dto.response;

import java.util.List;


public record SourceReference(

        int sourceId,
        String section,
        List<String> paragraphNumbers,
        int startPage,
        int endPage
) {
}
