package com.rag.engine.controller;

import com.rag.engine.service.ingestion.DocumentProcessingService;
import com.rag.engine.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentProcessingService processingService;

    @Autowired
    RagService ragService;

    public DocumentController(
            DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UUID> upload(
            @RequestParam("file") MultipartFile file)
            throws IOException, SQLException {

        UUID documentId = processingService.processDocument(file);

        return ResponseEntity.ok(documentId);
    }

}
