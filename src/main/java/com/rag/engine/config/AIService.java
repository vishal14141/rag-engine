package com.rag.engine.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIService {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .temperature(0.1)
                                .numPredict(200)
                                .keepAlive("10m")
                                .numCtx(8192)
                )
                .build();
    }
}
