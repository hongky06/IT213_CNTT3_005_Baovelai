package org.example.hackathon_de05.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RAGService {
    public static final String FALLBACK_ANSWER = "Không đủ căn cứ trong tài liệu nội bộ.";
    private static final String CORPUS_LOCATION = "classpath:tai_lieu_noi_bo.md";
    private static final int MAX_CHUNK_LENGTH = 1200;
    private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+?)\\s*#*\\s*$");

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;
    private final ChatExecutionContext executionContext;
    private final double similarityThreshold;
    private volatile boolean ingested;

    public RAGService(VectorStore vectorStore, ResourceLoader resourceLoader,
                      ChatExecutionContext executionContext,
                      @Value("${app.rag.similarity-threshold:0.70}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
        this.executionContext = executionContext;
        this.similarityThreshold = similarityThreshold;
    }

    public synchronized int ingestPolicyCorpus() throws IOException {
        if (ingested) {
            return 0;
        }

        Resource corpus = resourceLoader.getResource(CORPUS_LOCATION);
        if (!corpus.exists()) {
            throw new IOException("Không tìm thấy corpus RAG tại src/main/resources/tai_lieu_noi_bo.md.");
        }
        String markdown;
        try (var input = corpus.getInputStream()) {
            markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<Document> documents = createDocuments(markdown);
        if (documents.isEmpty()) {
            throw new IOException("Corpus RAG không có nội dung để ingest.");
        }
        vectorStore.add(documents);
        ingested = true;
        return documents.size();
    }

    public RetrievalResult retrieve(String question) throws IOException {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Câu hỏi RAG không được để trống.");
        }
        ingestPolicyCorpus();

        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question.trim())
                        .topK(4)
                        .similarityThreshold(similarityThreshold)
                        .build());
        if (matches == null || matches.isEmpty()) {
            return new RetrievalResult(FALLBACK_ANSWER, List.of());
        }

        Map<String, SourceReference> sources = new LinkedHashMap<>();
        List<String> evidence = new ArrayList<>();
        for (Document document : matches) {
            String text = document.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            Map<String, Object> metadata = document.getMetadata();
            SourceReference source = new SourceReference(
                    document.getId(),
                    String.valueOf(metadata.getOrDefault("section", "unknown")),
                    String.valueOf(metadata.getOrDefault("source", CORPUS_LOCATION)));
            sources.putIfAbsent(source.id(), source);
            evidence.add("[" + source.section() + "; nguồn: " + source.source() + "]\n" + text);
        }

        if (evidence.isEmpty()) {
            return new RetrievalResult(FALLBACK_ANSWER, List.of());
        }
        sources.values().forEach(executionContext::recordSource);
        return new RetrievalResult(String.join("\n\n", evidence), List.copyOf(sources.values()));
    }

    private List<Document> createDocuments(String markdown) {
        List<Document> documents = new ArrayList<>();
        String section = "Tài liệu";
        StringBuilder sectionContent = new StringBuilder();

        for (String line : markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                addSectionDocuments(documents, section, sectionContent.toString());
                section = heading.group(2).trim();
                sectionContent.setLength(0);
            } else {
                sectionContent.append(line).append('\n');
            }
        }
        addSectionDocuments(documents, section, sectionContent.toString());
        return documents;
    }

    private void addSectionDocuments(List<Document> documents, String section, String content) {
        String normalizedSection = section.trim();
        String[] paragraphs = content.trim().split("\\n\\s*\\n");
        StringBuilder chunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            String cleaned = paragraph.replaceAll("\\s+", " ").trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            if (cleaned.length() > MAX_CHUNK_LENGTH) {
                if (!chunk.isEmpty()) {
                    addDocument(documents, normalizedSection, chunkIndex++, chunk.toString());
                    chunk.setLength(0);
                }
                for (int start = 0; start < cleaned.length(); start += MAX_CHUNK_LENGTH) {
                    int end = Math.min(start + MAX_CHUNK_LENGTH, cleaned.length());
                    addDocument(documents, normalizedSection, chunkIndex++, cleaned.substring(start, end));
                }
                continue;
            }
            if (!chunk.isEmpty() && chunk.length() + cleaned.length() + 1 > MAX_CHUNK_LENGTH) {
                addDocument(documents, normalizedSection, chunkIndex++, chunk.toString());
                chunk.setLength(0);
            }
            if (!chunk.isEmpty()) {
                chunk.append(' ');
            }
            chunk.append(cleaned);
        }
        if (!chunk.isEmpty()) {
            addDocument(documents, normalizedSection, chunkIndex, chunk.toString());
        }
    }

    private void addDocument(List<Document> documents, String section, int chunkIndex, String text) {
        String contentHash = sha256(text);
        String sectionId = sha256(section).substring(0, 12);
        String documentId = "policy-" + sha256(CORPUS_LOCATION + "|" + sectionId + "|" + chunkIndex + "|" + contentHash);
        documents.add(new Document(documentId, text, Map.of(
                "doc_id", documentId,
                "section", section,
                "source", "tai_lieu_noi_bo.md",
                "content_hash", contentHash)));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record SourceReference(String id, String section, String source) {
    }

    public record RetrievalResult(String evidence, List<SourceReference> sources) {
    }
}
