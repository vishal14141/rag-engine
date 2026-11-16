package com.rag.engine.service.retrieval.queryrewriting;

import com.rag.engine.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MultiQueryRewritingStrategy implements QueryRewritingStrategy{


    private final ChatClient chatClient;
    private final ObjectMapper mapper;

    public MultiQueryRewritingStrategy(ChatClient client, ObjectMapper mapper){
        this.chatClient = client;
        this.mapper = mapper;
    }

    @Override
    public String getName() {
        return "multi-query";
    }

    @Override
    public List<String> rewrite(String originalQuery) {

        if (originalQuery == null || originalQuery.isBlank()) {
            throw new IllegalArgumentException("Original query cannot be empty");
        }

        long start = System.currentTimeMillis();

        String response = chatClient.prompt()
                .system(Constant.QUERY_REWRITE_SYSTEM_PROMPT)
                .user("Original question: " + originalQuery)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("LLM returned an empty response");
        }

        List<String> rewrittenQueries = mapper.readValue(response, new TypeReference<>() {});

        List<String> result = rewrittenQueries.stream().filter(q -> q != null && !q.isBlank()).map(String::trim)
                .distinct().toList();

        if (result.isEmpty()) {
            throw new IllegalStateException("No valid rewritten queries generated");
        }

        log.info("Query rewriting completed. Generated {} queries in {} ms", result.size(), System.currentTimeMillis() - start);

        return result;
    }
}
