package com.rag.engine.service.retrieval.contextcompression;

import com.rag.engine.dto.ChunkParagraph;
import com.rag.engine.dto.CompressedSentence;
import com.rag.engine.service.embedding.EmbeddingService;
import com.rag.engine.service.retrieval.dto.RankedChunk;
import com.rag.engine.service.retrieval.dto.RetrievedChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmbeddingBasedContextCompressionStrategy implements ContextCompressionStrategy {


    private final EmbeddingService embeddingService;

    @Value("${rag.compression.similarity-threshold:0.70}")
    private double similarityThreshold;

    @Value("${rag.compression.max-sentences:30}")
    private int maxSentences;

    public EmbeddingBasedContextCompressionStrategy(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public String getName() {
        return "embedding-based";
    }

    @Override
    public List<CompressedSentence> compressContext(List<RankedChunk> chunks, String originalQuery) {

        PriorityQueue<CompressedSentence> pq = new PriorityQueue<>(Comparator.comparingDouble(CompressedSentence::similarityScore));

        float[] queryEmbedding = embeddingService.generateEmbedding(originalQuery);


        List<String> completeTextLineList = new ArrayList<>();
        List<SentenceRecord> sentenceRecords = new ArrayList<>();

        for (RankedChunk chunk : chunks) {

            RetrievedChunk retrievedChunk = chunk.chunk();
            List<ChunkParagraph> chunkParagraphs = retrievedChunk.paragraphs();


            for (ChunkParagraph paragraph : chunkParagraphs) {

                List<String> textLines = List.of(paragraph.content().split("(?<=[.!?])\\s+"));

                for (String textLine : textLines) {
                    if (textLine == null || textLine.isBlank()) continue;

                    completeTextLineList.add(textLine);
                    sentenceRecords.add(new SentenceRecord(retrievedChunk.id(), retrievedChunk.section(), paragraph.paragraphNumber(), paragraph.pageNumber()));
                }
            }
        }


        List<float[]> textLinesEmbedding = embeddingService.generateEmbeddings(completeTextLineList);

        if (textLinesEmbedding.size() != completeTextLineList.size()) {
            throw new IllegalStateException("Sentence and embedding counts do not match");
        }

        if (completeTextLineList.size() != sentenceRecords.size()) {
            throw new IllegalStateException("Sentence and metadata counts do not match");
        }

        for (int i = 0; i < completeTextLineList.size(); ++i) {
            double similarity = cosineSimilarity(queryEmbedding, textLinesEmbedding.get(i));
            if (similarity < similarityThreshold) continue;

            SentenceRecord sentenceRecord = sentenceRecords.get(i);

            pq.add(new CompressedSentence(sentenceRecord.chunkId, sentenceRecord.section, sentenceRecord.paragraphNumber, sentenceRecord.pageNumber, completeTextLineList.get(i), similarity));
            if (pq.size() > maxSentences) pq.poll();
        }

        List<CompressedSentence> compressedSentenceList = new ArrayList<>();
        while (!pq.isEmpty()) compressedSentenceList.add(pq.poll());

        Collections.reverse(compressedSentenceList);

        return compressedSentenceList;
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            throw new IllegalArgumentException("Zero vector encountered");
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }


    private record SentenceRecord(
            UUID chunkId,
            String section,
            String paragraphNumber,
            int pageNumber
    ) {
    }

}


