package com.rag.engine.controller;

import com.rag.engine.service.ingestion.document.PdfExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    PdfExtractionService pdfExtractionService;

    @PostMapping(
            value = "/test",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<String> test(@RequestParam("file") MultipartFile file) throws IOException {
        return pdfExtractionService.extractTextByPage(file);
    }
}
