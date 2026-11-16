package com.rag.engine.service;

import com.rag.engine.config.RetrievalProperties;
import com.rag.engine.constants.Constant;
import com.rag.engine.dto.CompressedSentence;
import com.rag.engine.dto.response.RagResponse;
import com.rag.engine.dto.response.SourceReference;
import com.rag.engine.service.retrieval.RetrievalOrchestrator;
import com.rag.engine.service.retrieval.contextcompression.ContextCompressionStrategy;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.fusion.FusionOrchestrator;
import com.rag.engine.service.retrieval.queryrewriting.QueryRewritingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
    private final QueryRewritingStrategy queryRewritingStrategy;
    private final ContextCompressionStrategy contextCompressionStrategy;

    public RagService(RetrievalOrchestrator retrievalOrchestrator, FusionOrchestrator fusionOrchestrator, ChatClient chatClient, RetrievalProperties retrievalProperties, QueryRewritingStrategy queryRewritingStrategy, ContextCompressionStrategy contextCompressionStrategy) {
        this.retrievalOrchestrator = retrievalOrchestrator;
        this.fusionOrchestrator = fusionOrchestrator;
        this.chatClient = chatClient;
        this.retrievalProperties = retrievalProperties;
        this.queryRewritingStrategy = queryRewritingStrategy;
        this.contextCompressionStrategy = contextCompressionStrategy;
    }


    public RagResponse ask(String query) {

        if (query == null || query.isBlank()) throw new IllegalArgumentException("Query is empty or null");

        // 1: Query rewriting
        List<String> queries = new ArrayList<>();
        queries.add(query);
        queries.addAll(queryRewritingStrategy.rewrite(query));

        log.info("Total queries: {}", queries.size());

        // 2. Retrieval
        List<Map<String, List<RankedChunk>>> retrievedChunkMapList = retrievalOrchestrator.retrieve(queries);

        // 3. Fusion
        List<RankedChunk> rankedChunks = fusionOrchestrator.fuse(retrievedChunkMapList);

        if (rankedChunks.isEmpty())
            return new RagResponse("I don't have enough information to answer this question", List.of());

        // 4. Select top-ranked chunks
        int finalTopK = Math.min(retrievalProperties.getFinalTopK(), rankedChunks.size());
        List<RankedChunk> topFinalRankedChunks = rankedChunks.subList(0, finalTopK);

        // 5. Context compression
        List<CompressedSentence> compressedSentences = contextCompressionStrategy.compressContext(topFinalRankedChunks, query);

        if (compressedSentences.isEmpty()) {
            return new RagResponse(
                    "I don't have enough information to answer this question",
                    List.of()
            );
        }

        // 6. Group compressed sentences by source chunk
        Map<UUID, List<CompressedSentence>> sentencesByChunk =
                compressedSentences.stream()
                        .collect(Collectors.groupingBy(
                                CompressedSentence::chunkId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));


        // 7. Build context and source references
        StringBuilder context = new StringBuilder();
        List<SourceReference> sources = new ArrayList<>();

        int sourceId = 1;

        for (Map.Entry<UUID, List<CompressedSentence>> entry :
                sentencesByChunk.entrySet()) {

            List<CompressedSentence> sentences = entry.getValue();

            CompressedSentence firstSentence = sentences.get(0);

            List<String> paragraphNumbers = sentences.stream()
                    .map(CompressedSentence::paragraphNumber)
                    .distinct()
                    .toList();

            List<String> excerpts = sentences.stream()
                    .map(CompressedSentence::content)
                    .toList();

            int pageNumber = sentences.stream()
                    .mapToInt(CompressedSentence::pageNumber)
                    .min()
                    .orElseThrow();

            int endPage = sentences.stream()
                    .mapToInt(CompressedSentence::pageNumber)
                    .max()
                    .orElseThrow();

            context.append("[Source ").append(sourceId).append("]\n")
                    .append("Section: ").append(firstSentence.section()).append("\n")
                    .append("Paragraphs: ").append(paragraphNumbers).append("\n")
                    .append("Content:\n");

            for (String excerpt : excerpts) {
                context.append("- ").append(excerpt).append("\n");
            }

            context.append("\n");

            sources.add(new SourceReference(
                    sourceId,
                    firstSentence.section(),
                    paragraphNumbers,
                    pageNumber,
                    endPage,
                    excerpts
            ));

            sourceId++;
        }

        // 8. Generate answer
        String prompt = Constant.PROMPT_TEMPLATE_LLM.formatted(context, query);

        var start = System.currentTimeMillis();
        String answer = chatClient.prompt().user(prompt).call().content();
        log.info("LLM Response time: {} ms", System.currentTimeMillis() - start);

        return new RagResponse(answer, sources);
    }
}
