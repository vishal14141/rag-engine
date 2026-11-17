package com.rag.engine.service.ingestion.dto;

public record DocumentMetadata (

        String fileName,
        String documentTitle,
        String documentType,
        String fileHash
) {}