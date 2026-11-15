package com.rag.engine.service;

import com.rag.engine.config.RetrievalProperties;
import com.rag.engine.constants.Constant;
import com.rag.engine.dto.response.RagResponse;
import com.rag.engine.dto.response.SourceReference;
import com.rag.engine.service.retrieval.RetrievalOrchestrator;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import com.rag.engine.service.retrieval.fusion.FusionOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * used for orchestrating retrieval.
 * so user query comes to it, it uses @RetrievalService to get tok K for the query.
 * send those tok K to LLM for answer generation.
 * send response back
 */


@Slf4j
@Service
public class RagService {

    private final RetrievalOrchestrator retrievalOrchestrator;
    private final FusionOrchestrator fusionOrchestrator;
    private final ChatClient chatClient;
    private final RetrievalProperties retrievalProperties;

    public RagService(RetrievalOrchestrator retrievalOrchestrator, FusionOrchestrator fusionOrchestrator, ChatClient chatClient, RetrievalProperties retrievalProperties) {
        this.retrievalOrchestrator = retrievalOrchestrator;
        this.fusionOrchestrator = fusionOrchestrator;
        this.chatClient = chatClient;
        this.retrievalProperties = retrievalProperties;
    }


    public RagResponse ask(String query) {

        if (query == null || query.isBlank()) throw new IllegalArgumentException("Query is empty or null");

        Map<String, List<RankedChunk>> retrievedChunkMap = retrievalOrchestrator.retrieve(query);
        List<RankedChunk> rankedChunks = fusionOrchestrator.fuse(retrievedChunkMap);

        int finalTopK = Math.min(retrievalProperties.getFinalTopK(), rankedChunks.size());

        List<RankedChunk> topFinalRankedChunks = rankedChunks.subList(0, finalTopK);

        if (retrievedChunkMap.isEmpty())
            return new RagResponse("I don't have enough information to answer this question", List.of());

        StringBuilder context = new StringBuilder();
        List<SourceReference> sources = new ArrayList<>();

        for (int i = 0; i < topFinalRankedChunks.size(); ++i) {

            RankedChunk rankedChunk = topFinalRankedChunks.get(i);
            RetrievedChunk retrievedChunk = rankedChunk.chunk();

            context.append("[Source ").append(i + 1).append("]\n")
                    .append("Section: ").append(retrievedChunk.section()).append("\n")
                    .append("Paragraphs: ").append(retrievedChunk.paragraphNumbers()).append("\n")
                    .append("Content: ").append(retrievedChunk.content()).append("\n\n");

            sources.add(new SourceReference(i + 1, retrievedChunk.section(), retrievedChunk.paragraphNumbers(), retrievedChunk.startPage(), retrievedChunk.endPage()));
        }

        String prompt = Constant.PROMPT_TEMPLATE.formatted(context, query);

        var start = System.currentTimeMillis();
        String answer = chatClient.prompt().user(prompt).call().content();
        log.info("LLM Response time: {} ms", System.currentTimeMillis() - start);

        return new RagResponse(answer, sources);
    }
}
