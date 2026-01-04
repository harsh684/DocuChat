package com.ai.docuchat.controller;

import com.ai.docuchat.service.IngestionService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/ingestion")
public class DocumentController {

    private IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload-pdf")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            // Convert MultipartFile to Resource
            InputStreamResource resource = new InputStreamResource(file.getInputStream());

            // Ingest the file
            ingestionService.loadPdf(resource);

            return ResponseEntity.ok("File '" + file.getOriginalFilename() + "' processed and indexed successfully!");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to process file: " + e.getMessage());
        }
    }
}
