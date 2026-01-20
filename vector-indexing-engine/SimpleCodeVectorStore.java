package com.example.vector.impl;

import com.example.vector.CodeVectorStore;
import com.example.vector.domain.CodeChunk;
import com.example.vector.domain.Kind;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SimpleCodeVectorStore implements CodeVectorStore {

    private final SimpleVectorStore vectorStore;

    public SimpleCodeVectorStore(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Insert (append) a CodeChunk into the vector store.
     * NOTE: SimpleVectorStore does NOT support true upsert.
     */
    @Override
    public void upsert(CodeChunk chunk) {

        Map<String, Object> metadata = new HashMap<>();

        if (chunk.metadata() != null) {
            metadata.putAll(chunk.metadata());
        }

        metadata.put("symbol", chunk.symbol());
        metadata.put("kind", chunk.kind().name());
        metadata.put("hash", chunk.hash());

        Document document = new Document(
                chunk.content(),   // vectorized text
                metadata
        );

        vectorStore.add(List.of(document));
    }

    /**
     * Semantic similarity search with post-filtering.
     */
    @Override
    public List<CodeChunk> search(String query, int k, Map<String, Object> filter) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .build();

        return vectorStore.similaritySearch(request)
                .stream()
                .filter(doc -> matchesFilter(doc.getMetadata(), filter))
                .map(this::toCodeChunk)
                .collect(Collectors.toList());
    }

    /* ----------------------------------------------------- */
    /* ----------------- Helper methods -------------------- */
    /* ----------------------------------------------------- */

    private boolean matchesFilter(
            Map<String, Object> metadata,
            Map<String, Object> filter
    ) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }

        return filter.entrySet().stream()
                .allMatch(e ->
                        e.getValue().equals(metadata.get(e.getKey()))
                );
    }

    private CodeChunk toCodeChunk(Document doc) {

        Map<String, Object> metadata = doc.getMetadata();

        return new CodeChunk(
                (String) metadata.get("symbol"),
                Kind.valueOf((String) metadata.get("kind")),
                doc.getText(),          // ✅ correct Spring AI API
                metadata,
                (String) metadata.get("hash")
        );
    }
}
