
package com.example.vector.service;

import com.example.vector.model.VectorDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VectorIndexService {

    private final VectorStore store;

    public VectorIndexService(VectorStore store) {
        this.store = store;
    }

    public void upsert(VectorDocument doc) {
        store.add(List.of(
            new org.springframework.ai.document.Document(
                doc.getId(),
                doc.getContent(),
                doc.getMetadata()
            )
        ));
    }

    public List<Document> search(
            String query,
            int k,
            Map<String, Object> filter) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .filterExpression(String.valueOf(filter))
                .build();

        return store.similaritySearch(request);
    }

    public void deleteById(String id) {
        store.delete(List.of(id));
    }
}
