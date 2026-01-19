@Configuration
public class VectorConfig {

    @Bean
    public VectorIndex vectorIndex(EmbeddingModel model) throws Exception {
        return new SimpleFileVectorStore(
                model,
                Path.of("data/vectors.json")
        );
    }
}


// spring:
//   ai:
//     openai:
//       api-key: ${OPENAI_API_KEY}
//       embedding:
//         model: text-embedding-3-large
