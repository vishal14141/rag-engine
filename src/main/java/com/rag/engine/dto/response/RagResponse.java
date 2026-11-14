package com.rag.engine.dto.response;

import java.util.List;


public record RagResponse(

        String answer,
        List<SourceReference> sources
) {
}
