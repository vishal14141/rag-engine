package com.rag.engine.service.ingestion.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PdfExtractionService {

    public List<String> extractTextByPage(MultipartFile file)
            throws IOException {

        List<String> pages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();

            int totalPages = document.getNumberOfPages();

            log.info("Total pages : {}", totalPages);

            for (int i = 1; i <= totalPages; i++) {

                stripper.setStartPage(i);
                stripper.setEndPage(i);

                String pageText = stripper.getText(document);

                pages.add(pageText);
            }
        }

        return pages;
    }


}
