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

        Document document = new Document(
                chunk.content(),
                enrichMetadata(chunk)
        );

        vectorStore.add(List.of(document));
    }

    @Override
    public List<CodeChunk> search(String query, int k, Map<String, Object> filter) {

        SearchRequest request = SearchRequest.query(query)
                .withTopK(k);

        // SimpleVectorStore supports metadata filtering via predicate
        if (filter != null && !filter.isEmpty()) {
            request = request.withFilterExpression(
                    metadata -> filter.entrySet().stream()
                            .allMatch(e ->
                                    e.getValue().equals(metadata.get(e.getKey()))
                            )
            );
        }

        return vectorStore.similaritySearch(request)
                .stream()
                .map(this::toCodeChunk)
                .collect(Collectors.toList());
    }

    private CodeChunk toCodeChunk(Document doc) {
        return new CodeChunk(
                (String) doc.getMetadata().get("id"),
                doc.getContent(),
                doc.getMetadata()
        );
    }

    private Map<String, Object> enrichMetadata(CodeChunk chunk) {
        Map<String, Object> metadata =
                chunk.metadata() == null
                        ? new java.util.HashMap<>()
                        : new java.util.HashMap<>(chunk.metadata());

        metadata.put("id", chunk.id());

        return metadata;
    }
}
