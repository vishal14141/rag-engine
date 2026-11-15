package com.rag.engine.service.retrieval.queryrewriting;

import java.util.List;

public interface QueryRewritingStrategy {

    String getName();
    List<String> rewrite(String query);
}
