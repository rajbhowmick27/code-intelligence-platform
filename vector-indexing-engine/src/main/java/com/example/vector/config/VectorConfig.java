
package com.example.vector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.chroma.ChromaVectorStore;

@Configuration
public class VectorConfig {

    @Bean
    public EmbeddingClient embeddingClient(OpenAiApi api) {
        return new OpenAiEmbeddingClient(api);
    }

    @Bean
    public ChromaVectorStore chromaVectorStore(EmbeddingClient client) {
        return new ChromaVectorStore(client);
    }
}
