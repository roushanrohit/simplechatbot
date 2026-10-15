package com.chatbot.simplechatbot.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ChatService {

    private static final String MODEL = "gpt-4.1-mini";

    private static final String DEFAULT_SYSTEM_MESSAGE =
            "You are a helpful assistant.";

    private static final String OFFICE_SYSTEM_MESSAGE =
            "You are a helpful assistant. After every response, "
                    + "remind the user to exercise and suggest a different "
                    + "exercise each time.";

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

        // Find the latest user message
        String latestUserMessage = history.stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .map(UserMessage::singleText)
                .reduce((first, second) -> second)
                .orElse("");

        // Select the system prompt dynamically
        String systemMessage;

        if (latestUserMessage.toLowerCase(Locale.ROOT).matches("(?s).*\\boffice\\b.*")) {
            systemMessage = OFFICE_SYSTEM_MESSAGE;
        } else {
            systemMessage = DEFAULT_SYSTEM_MESSAGE;
        }

        // Add the system prompt before the conversation history
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(systemMessage));
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
