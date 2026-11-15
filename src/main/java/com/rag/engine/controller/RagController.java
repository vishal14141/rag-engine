package com.rag.engine.controller;

import com.rag.engine.dto.response.RagResponse;
import com.rag.engine.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagController {


    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/ask")
    public ResponseEntity<RagResponse> ask(@RequestParam String question) {
        log.info("User question : {}", question);
        return ResponseEntity.ok(ragService.ask(question));
    }
}
