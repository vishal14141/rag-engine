package com.rag.engine.service.ingestion.chunking;

import com.rag.engine.dto.DocumentChunk;
import com.rag.engine.dto.ExtractedParagraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ChunkingService {

    public static final int MAX_TOKEN_SIZE_LIMIT_PER_CHUNK = 500;

    public List<DocumentChunk> createChunks(
            List<ExtractedParagraph> paragraphs) {

        List<DocumentChunk> chunks = new ArrayList<>();
        DocumentChunk currentChunk = null;

        for (ExtractedParagraph paragraph : paragraphs) {

            if (paragraph == null || paragraph.getText() == null
                    || paragraph.getText().isBlank()) {
                continue;
            }

            List<String> sentences = splitParagraph(paragraph.getText());

            for (String sentence : sentences) {

                List<String> units = splitOversizedText(sentence);

                for (String unit : units) {

                    if (unit.isBlank()) {
                        continue;
                    }

                    boolean differentSection = currentChunk != null
                            && !Objects.equals(
                            currentChunk.getSection(),
                            paragraph.getSection());

                    boolean exceedsLimit = currentChunk != null
                            && estimateTokens(
                            currentChunk.getContent() + " " + unit)
                            > MAX_TOKEN_SIZE_LIMIT_PER_CHUNK;

                    if (differentSection || exceedsLimit) {
                        chunks.add(currentChunk);
                        currentChunk = null;
                    }

                    if (currentChunk == null) {
                        currentChunk = new DocumentChunk(
                                paragraph.getSection(),
                                "",
                                new ArrayList<>(),
                                paragraph.getPageNumber(),
                                paragraph.getPageNumber(),
                                0
                        );
                    }

                    // Preserve paragraph metadata without duplicates.
                    if (!currentChunk.getParagraphNumbers()
                            .contains(paragraph.getParagraphNumber())) {

                        currentChunk.getParagraphNumbers()
                                .add(paragraph.getParagraphNumber());
                    }

                    String content = currentChunk.getContent();

                    currentChunk.setContent(
                            content.isEmpty()
                                    ? unit
                                    : content + " " + unit
                    );

                    currentChunk.setTokenCount(
                            estimateTokens(currentChunk.getContent())
                    );

                    currentChunk.setEndPage(
                            Math.max(
                                    currentChunk.getEndPage(),
                                    paragraph.getPageNumber()
                            )
                    );
                }
            }
        }

        if (currentChunk != null) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    private List<String> splitParagraph(String text) {

        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(sentence -> !sentence.isBlank())
                .toList();
    }

    private List<String> splitOversizedText(String text) {

        if (estimateTokens(text) <= MAX_TOKEN_SIZE_LIMIT_PER_CHUNK) {
            return List.of(text);
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        String[] words = text.trim().split("\\s+");

        for (String word : words) {

            // Handle an exceptionally long individual word.
            if (estimateTokens(word) > MAX_TOKEN_SIZE_LIMIT_PER_CHUNK) {

                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }

                result.addAll(splitOversizedWord(word));
                continue;
            }

            String candidate = current.isEmpty()
                    ? word
                    : current + " " + word;

            if (estimateTokens(candidate) > MAX_TOKEN_SIZE_LIMIT_PER_CHUNK) {

                result.add(current.toString());
                current.setLength(0);
                current.append(word);

            } else {

                if (!current.isEmpty()) {
                    current.append(" ");
                }

                current.append(word);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private List<String> splitOversizedWord(String word) {

        List<String> result = new ArrayList<>();

        int start = 0;
        int maxCharacters = MAX_TOKEN_SIZE_LIMIT_PER_CHUNK * 4;

        while (start < word.length()) {

            int end = Math.min(start + maxCharacters, word.length());

            // Avoid breaking Unicode surrogate pairs.
            if (end < word.length()
                    && Character.isHighSurrogate(word.charAt(end - 1))
                    && Character.isLowSurrogate(word.charAt(end))) {
                end--;
            }

            result.add(word.substring(start, end));
            start = end;
        }

        return result;
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }
}
