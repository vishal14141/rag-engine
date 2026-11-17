package com.rag.engine.service.ingestion.dto;

public record DocumentUploadResult(String fileName,
                                   String documentTitle,
                                   DocumentUploadStatus status,
                                   String message
) {
}