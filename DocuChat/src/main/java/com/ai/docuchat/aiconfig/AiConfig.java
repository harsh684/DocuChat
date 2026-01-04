package com.ai.docuchat.aiconfig;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Configuration
public class AiConfig {

    @Value("${spring.ai.huggingface.embedding.api-key}")
    private String apiKey;

    @Value("${spring.ai.huggingface.embedding.url}")
    private String url;

//    @Bean
//    @Primary
//    public OpenAiChatModel primaryChatModel(OpenAiChatModel openAiChatModel) {
//        return openAiChatModel;
//    }

    @Bean
    public AbstractEmbeddingModel embeddingModel() {
        RestClient restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        return new AbstractEmbeddingModel() {

            // Fix 1: Use getText() instead of getContent()
            @Override
            public float[] embed(Document document) {
                return embed(document.getText());
            }

            // Fix 2: Implement the required String-to-float[] method
            @Override
            public float[] embed(String text) {
                EmbeddingResponse response = call(new EmbeddingRequest(List.of(text), null));
                return response.getResult().getOutput();
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                // 2. Prepare the payload: {"inputs": ["chunk1", "chunk2", ...]}
                // We send the instructions (text chunks) to the HF Feature Extraction pipeline.
                List<List<Double>> apiResponse = restClient.post()
                        .body(Map.of("inputs", request.getInstructions()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<List<Double>>>() {});

                if (apiResponse == null) return new EmbeddingResponse(List.of());

                // 3. Data Transformation
                // Hugging Face returns List<List<Double>>.
                // Spring AI's 'Embedding' object requires a primitive float[] array.
                List<Embedding> embeddings = apiResponse.stream().map(doubleList -> {
                    float[] floatArray = new float[doubleList.size()];
                    for (int i = 0; i < doubleList.size(); i++) {
                        // Cast Double to float for the vector
                        floatArray[i] = doubleList.get(i).floatValue();
                    }
                    // Return a new Embedding object with index 0
                    return new Embedding(floatArray, 0);
                }).toList();

                return new EmbeddingResponse(embeddings);
            }
        };
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(5) // Number of messages to remember in the conversation window
                .build();
    }

}
