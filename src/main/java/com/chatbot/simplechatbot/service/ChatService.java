package com.chatbot.simplechatbot.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private static final String MODEL = "gpt-4.1-mini";
    private static final String SYSTEM_MESSAGE = "You are a helpful assistant";

    private final ChatModel model;

    public ChatService() {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY environment variable is not set"
            );
        }

        // builder design pattern
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(MODEL)
                .build();
    }

    public String chat(List<ChatMessage> history) {

        // Add the system prompt before the conversation history
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(SYSTEM_MESSAGE));
        messages.addAll(history);

        // Build the LangChain4j request
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();

        // Call the LLM
        ChatResponse response = model.chat(request);

        return response.aiMessage().text();
    }
}
