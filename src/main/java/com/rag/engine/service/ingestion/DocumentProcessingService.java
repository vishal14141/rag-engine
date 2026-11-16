package com.rag.engine.service.ingestion;

import com.rag.engine.dto.DocumentChunk;
import com.rag.engine.dto.ExtractedParagraph;
import com.rag.engine.service.embedding.EmbeddingService;
import com.rag.engine.service.ingestion.chunking.ChunkingService;
import com.rag.engine.service.ingestion.document.DocumentStorageService;
import com.rag.engine.service.ingestion.document.ParagraphExtractionService;
import com.rag.engine.service.ingestion.document.PdfExtractionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentProcessingService {

    private final PdfExtractionService pdfExtractionService;
    private final ParagraphExtractionService paragraphExtractionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentStorageService documentStorageService;

    public DocumentProcessingService(PdfExtractionService pdfExtractionService, ParagraphExtractionService paragraphExtractionService, ChunkingService chunkingService, EmbeddingService embeddingService, DocumentStorageService documentStorageService) {
        this.pdfExtractionService = pdfExtractionService;
        this.paragraphExtractionService = paragraphExtractionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.documentStorageService = documentStorageService;
    }

    public UUID processDocument(MultipartFile file) throws IOException, SQLException {

        // 1. Extract PDF text
        List<String> pages = pdfExtractionService.extractTextByPage(file);

        // 2. Extract structured paragraphs
        List<ExtractedParagraph> paragraphs = paragraphExtractionService.extractParagraphs(pages);

        // 3. Generate chunks
        List<DocumentChunk> chunks = chunkingService.createChunks(paragraphs);

        // 4. Generate embeddings
        List<String> texts = chunks.stream().map(DocumentChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        // 5. Persist document and chunks
        return documentStorageService.saveDocument(file.getOriginalFilename(), chunks, embeddings);
    }
}
