package com.rag.engine.service.embedding;

import com.rag.engine.dto.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rag.engine.constants.Constant.EMBEDDING_BATCH_SIZE;

@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(String text) {

        if (text == null || text.isBlank()) throw new IllegalArgumentException("Text cannot be null or empty");

        float[] embedding = embeddingModel.embed(text);

        if (embedding.length == 0) throw new IllegalStateException("Embedding generation returned an empty vector");

        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                throw new IllegalStateException("Embedding contains an invalid numerical value");
            }
        }

        return embedding;
    }

    public List<float[]> generateEmbeddings(List<String> texts) {

        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Generating embedding for list size: {}", texts.size());

        List<float[]> embeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += EMBEDDING_BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, texts.size()));
            embeddings.addAll(embeddingModel.embed(batch));
            log.info("Embedding Batch {} of size {} completed", i / EMBEDDING_BATCH_SIZE, batch.size());
        }

        log.info("Generated embedding for list size: {}", embeddings.size());

        if (embeddings.size() != texts.size()) {
            throw new IllegalStateException(
                    "Embedding count does not match chunk count"
            );
        }
        return embeddings;
    }

}
