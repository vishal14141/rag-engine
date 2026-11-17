package com.rag.engine.service.ingestion;

import com.rag.engine.dto.DocumentChunk;
import com.rag.engine.dto.ExtractedParagraph;
import com.rag.engine.repository.DocumentMetadataRepository;
import com.rag.engine.service.embedding.EmbeddingService;
import com.rag.engine.service.ingestion.chunking.ChunkingService;
import com.rag.engine.service.ingestion.document.DocumentStorageServiceImpl;
import com.rag.engine.service.ingestion.document.ParagraphExtractionService;
import com.rag.engine.service.ingestion.document.PdfExtractionService;
import com.rag.engine.service.ingestion.dto.DocumentMetadata;
import com.rag.engine.service.ingestion.dto.DocumentUploadResult;
import com.rag.engine.service.ingestion.dto.DocumentUploadStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
public class DocumentIngestionService {

    private final DocumentMetadataExtractor metadataExtractor;
    private final DocumentMetadataRepository metadataRepository;

    private final PdfExtractionService pdfExtractionService;
    private final ParagraphExtractionService paragraphExtractionService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentStorageServiceImpl documentStorageServiceImpl;

    public DocumentIngestionService(
            DocumentMetadataExtractor metadataExtractor,
            DocumentMetadataRepository metadataRepository,
            PdfExtractionService pdfExtractionService,
            ParagraphExtractionService paragraphExtractionService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            DocumentStorageServiceImpl documentStorageServiceImpl
    ) {
        this.metadataExtractor = metadataExtractor;
        this.metadataRepository = metadataRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.paragraphExtractionService = paragraphExtractionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.documentStorageServiceImpl = documentStorageServiceImpl;
    }

    /**
     * Processes a single document.
     */
    public DocumentUploadResult processDocument(MultipartFile file) {

        return processDocument(
                file,
                new HashSet<>(),
                new HashSet<>()
        );
    }

    /**
     * Processes multiple documents.
     *
     * Batch-level sets are shared across all files in this request.
     */
    public List<DocumentUploadResult> processDocuments(
            List<MultipartFile> files
    ) {

        if (files == null || files.isEmpty()) {
            return List.of();
        }

        Set<String> batchHashes = new HashSet<>();
        Set<String> batchTitles = new HashSet<>();

        List<DocumentUploadResult> results = new ArrayList<>();

        for (MultipartFile file : files) {

            DocumentUploadResult result = processDocument(
                    file,
                    batchHashes,
                    batchTitles
            );

            results.add(result);
        }

        return results;
    }

    /**
     * Internal processing method.
     *
     * Receives shared batch-level sets when processing multiple files.
     */
    private DocumentUploadResult processDocument(
            MultipartFile file,
            Set<String> batchHashes,
            Set<String> batchTitles
    ) {

        String fileName = file != null
                ? file.getOriginalFilename()
                : null;

        String documentTitle = null;
        String fileHash = null;

        boolean reservedInBatch = false;

        try {

            // 1. Validate file.

            if (file == null || file.isEmpty()) {

                return new DocumentUploadResult(
                        fileName,
                        null,
                        DocumentUploadStatus.FAILED,
                        "Uploaded file is empty or invalid"
                );
            }

            // 2. Extract document metadata.

            DocumentMetadata metadata =
                    metadataExtractor.extractDocMetadata(file);

            documentTitle = metadata.documentTitle();
            fileHash = metadata.fileHash();

            String normalizedTitle = normalizeTitle(documentTitle);

            // 3. Check database-level duplicate using file hash.

            if (metadataRepository.existsByFileHash(fileHash)) {

                return new DocumentUploadResult(
                        fileName,
                        documentTitle,
                        DocumentUploadStatus.DUPLICATE,
                        "An identical file already exists in the database"
                );
            }

            // 4. Check database-level duplicate using document title.

            if (!normalizedTitle.isBlank()
                    && metadataRepository.existsByDocumentTitle(documentTitle)) {

                return new DocumentUploadResult(
                        fileName,
                        documentTitle,
                        DocumentUploadStatus.DUPLICATE,
                        "A document with the same title already exists in the database"
                );
            }

            // 5. Check batch-level duplicate using file hash.

            if (batchHashes.contains(fileHash)) {

                return new DocumentUploadResult(
                        fileName,
                        documentTitle,
                        DocumentUploadStatus.DUPLICATE,
                        "An identical file already exists in the current upload batch"
                );
            }

            // 6. Check batch-level duplicate using document title.

            if (!normalizedTitle.isBlank()
                    && batchTitles.contains(normalizedTitle)) {

                return new DocumentUploadResult(
                        fileName,
                        documentTitle,
                        DocumentUploadStatus.DUPLICATE,
                        "A document with the same title already exists in the current upload batch"
                );
            }

            // 7. Reserve this document in the current batch.

            batchHashes.add(fileHash);

            if (!normalizedTitle.isBlank()) {
                batchTitles.add(normalizedTitle);
            }

            reservedInBatch = true;

            /*
             * NEXT STEP:
             *
             * Actual ingestion will happen here:
             *
             * 1. Extract paragraphs.
             * 2. Generate chunks.
             * 3. Generate embeddings.
             * 4. Persist document metadata.
             * 5. Persist chunks and paragraph metadata.
             *
             * Return SUCCESS only after persistence succeeds.
             */

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
            documentStorageServiceImpl.saveDocument(
                    metadata,
                    chunks,
                    embeddings
            );

            log.info("Document '{}' chunks saved in database", fileName);

            return new DocumentUploadResult(
                    fileName,
                    documentTitle,
                    DocumentUploadStatus.SUCCESS,
                    "Document chunks saved in database"
            );

        } catch (Exception exception) {

            // Remove batch reservation if processing failed.
            // This allows another occurrence in the same batch to be tried.

            if (reservedInBatch) {

                batchHashes.remove(fileHash);

                String normalizedTitle = normalizeTitle(documentTitle);

                if (!normalizedTitle.isBlank()) {
                    batchTitles.remove(normalizedTitle);
                }
            }
            log.info("Document processing failed, fileName: {}", fileName);
            log.error("Error message : {}", exception.getMessage());

            return new DocumentUploadResult(
                    fileName,
                    documentTitle,
                    DocumentUploadStatus.FAILED,
                    "Document processing failed: " + exception.getMessage()
            );
        }
    }

    private String normalizeTitle(String title) {

        if (title == null) {
            return "";
        }

        return title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}