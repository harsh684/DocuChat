package com.ai.docuchat.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem("""
                You are a helpful document assistant for 'DocuChat'. 
                Use only the provided context from the PDF to answer questions.
                If the answer is not in the context, say you don't know as it is not 
                provided in the context. 
                Keep answers concise and professional. Do not return the thinking part just provide the answer.
                """)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore).build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> chatStream(@RequestParam String message, @RequestParam String chatId) {
        return this.chatClient.prompt()
                .user(message)
                // this links the user session to the memory advisor
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .options(OpenAiChatOptions.builder()
                        .model("deepseek-ai/DeepSeek-R1")
                        .temperature(0.7)
                        .maxTokens(500)
                        .build())
                .stream()
                .content();
    }
}
