package com.rag.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "rag.retrieval")
public class RetrievalProperties {

    private List<StrategyConfig> strategies = new ArrayList<>();
    private FusionConfig fusion = new FusionConfig();

    private int finalTopK = 5;

    @Data
    public static class StrategyConfig {

        private String name;
        private boolean enabled = true;
        private int candidateLimit = 10;
    }

    @Data
    public static class FusionConfig {

        private String strategy = "rrf";
        private int rrfK = 60;
        private int candidateLimit = 10;
    }
}
