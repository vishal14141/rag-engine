package com.rag.engine.service.ingestion;

import com.rag.engine.service.ingestion.dto.DocumentMetadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service
public class DocumentMetadataExtractor {


    private final FileHashService fileHashService;

    public DocumentMetadataExtractor(FileHashService fileHashService) {
        this.fileHashService = fileHashService;
    }

    public DocumentMetadata extractDocMetadata(MultipartFile file) throws IOException, NoSuchAlgorithmException {

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }

        String documentType = "ANNUAL_REPORT";
        String fileHash = fileHashService.calculateHash(file, "SHA-256");

        StringBuilder docTitle = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            if (document.getNumberOfPages() < 3) {
                throw new IllegalArgumentException("Invalid RBI annual report PDF");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(3);

            String pageText = stripper.getText(document);

            String[] pageLines = pageText.split("\\R");
            String regex = "\\d{4}-\\d{2}";

            for (String line : pageLines) {

                String trimmedLine = line.trim();

                if (trimmedLine.isBlank()) {
                    continue;
                }

                if (trimmedLine.equalsIgnoreCase(
                        "RESERVE BANK OF INDIA ANNUAL REPORT")
                        || trimmedLine.matches(regex)) {

                    docTitle.append(trimmedLine).append(" ");
                }
            }
        }

        String title = docTitle.toString().trim();

        if (title.isBlank()) {
            throw new IllegalArgumentException(
                    "Could not extract RBI annual report title");
        }

        return new DocumentMetadata(
                fileName, title, documentType, fileHash);
    }
}
