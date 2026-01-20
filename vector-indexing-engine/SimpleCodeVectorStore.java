package com.example.vector.impl;

import com.example.vector.CodeVectorStore;
import com.example.vector.domain.CodeChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SimpleCodeVectorStore implements CodeVectorStore {

    private final SimpleVectorStore vectorStore;

    public SimpleCodeVectorStore(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void upsert(CodeChunk chunk) {

        Map<String, Object> metadata =
                chunk.metadata() == null
                        ? new java.util.HashMap<>()
                        : new java.util.HashMap<>(chunk.metadata());

        metadata.put("id", chunk.id());

        Document document = new Document(chunk.content(), metadata);

        vectorStore.add(List.of(document));
    }

    @Override
    public List<CodeChunk> search(String query, int k, Map<String, Object> filter) {

        // ✅ Correct way to build SearchRequest
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .build();

        return vectorStore.similaritySearch(request)
                .stream()
                // ✅ manual filtering (SimpleVectorStore limitation)
                .filter(doc -> matchesFilter(doc.getMetadata(), filter))
                .map(this::toCodeChunk)
                .collect(Collectors.toList());
    }

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
        return new CodeChunk(
                (String) doc.getMetadata().get("id"),
                doc.getText(),              // ✅ correct method
                doc.getMetadata()
        );
    }
}
