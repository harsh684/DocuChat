package com.ai.docuchat.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionService {
    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void loadPdf(Resource pdfResource) {
        // 1. Read the PDF content
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
        List<Document> documents = pdfReader.get();

        // 2. Split into chunks
        // 200 tokens per chunk with 50 token overlap for context continuity
        TokenTextSplitter splitter = new TokenTextSplitter(200, 50, 5, 10000, true);
        List<Document> chunks = splitter.apply(documents);

        // 3. Store in pgVector (This automatically triggers your custom EmbeddingModel)
        vectorStore.accept(chunks);
    }
}
