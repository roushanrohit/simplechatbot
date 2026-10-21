package com.chatbot.simplechatbot.service;

import com.chatbot.simplechatbot.tool.TransactionTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
    private static final String SYSTEM_MESSAGE =
            "You are a helpful assistant. "
            + "When asked for an account's total transaction amount, "
            + "use the get_total_amount tool. "
            + "Do not invent transaction amounts.";

    private static final int MAX_TOOL_ROUNDS = 5;
    private final ChatModel model;
    private final TransactionTool transactionTool;
    private final ObjectMapper objectMapper;
    private final List<ToolSpecification> toolSpecifications;

    public ChatService(TransactionTool transactionTool, ObjectMapper objectMapper) {
        this.transactionTool = transactionTool;
        this.objectMapper = objectMapper;
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }
        this.model = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(MODEL)
                        .build();
        this.toolSpecifications = ToolSpecifications.toolSpecificationsFrom(transactionTool);
    }

    public String chat(List<ChatMessage> history) {

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_MESSAGE));
        messages.addAll(history);

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatRequest request = ChatRequest.builder()
                                .messages(messages)
                                .toolSpecifications(toolSpecifications)
                                .build();
            ChatResponse response = model.chat(request);

            var aiMessage = response.aiMessage();

            // No tool call: return the final answer.
            if (aiMessage.toolExecutionRequests() == null
                    || aiMessage.toolExecutionRequests().isEmpty()) {
                return aiMessage.text();
            }

            // Add the assistant's tool-call message to the history.
            messages.add(aiMessage);

            // Execute every tool requested by the model.
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                String result = executeTool(toolRequest);
                messages.add(ToolExecutionResultMessage.from(toolRequest, result));
            }
        }

        throw new IllegalStateException("Maximum tool-calling rounds exceeded");
    }

    private String executeTool(ToolExecutionRequest toolRequest) {

        // The @Tool annotation normally exposes the Java method name.
        // If you configure a different tool name, update this mapping.
        if ("getTotalAmount".equals(toolRequest.name()) || "get_total_amount".equals(toolRequest.name())) {
            try {
                JsonNode arguments = objectMapper.readTree(toolRequest.arguments());
                JsonNode accountIdNode = arguments.get("accountId");
                if (accountIdNode == null || accountIdNode.asText().isBlank()) {
                    return "Invalid or missing account_id argument.";
                }
                return transactionTool.getTotalAmount(accountIdNode.asInt());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute transaction tool", e);
            }
        }

        throw new IllegalArgumentException("Unknown tool: " + toolRequest.name());
    }
}
