package com.chatbot.simplechatbot.service;

import com.chatbot.simplechatbot.tool.ImageGenerationTool;
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
            "You are a helpful AI assistant integrated with a database "
                + "and an image generation service. "

                + "You have access to two tools: "
                + "get_total_amount and generate_image. "

                + "DATABASE TOOL RULES: "
                + "When the user asks about the total transaction amount "
                + "for an account, use the get_total_amount tool. "
                + "Pass the correct account ID as an integer. "
                + "Never invent transaction amounts or claim that a database "
                + "query succeeded unless the tool returns a result. "

                + "IMAGE GENERATION RULES: "
                + "When the user explicitly asks to generate, create, draw, "
                + "or make an image or picture, use the generate_image tool. "
                + "Convert the user's request into a clear, detailed image "
                + "prompt when useful, while preserving their intent. "
                + "Do not merely describe an image when the user has asked "
                + "you to generate one. "
                + "After the tool returns, briefly tell the user that the "
                + "image has been generated. Do not expose Base64 image data "
                + "as ordinary text. "

                + "GENERAL CONVERSATION RULES: "
                + "For questions, explanations, coding help, and ordinary "
                + "conversation, answer directly without calling tools "
                + "unless a tool is genuinely needed. "
                + "Never claim to have generated an image or retrieved "
                + "database information unless the corresponding tool ran "
                + "successfully.";

    private static final int MAX_TOOL_ROUNDS = 5;
    private final ChatModel model;
    private final TransactionTool transactionTool;
    private final ObjectMapper objectMapper;
    private final List<ToolSpecification> toolSpecifications;
    private final ImageGenerationTool imageGenerationTool;

    public ChatService(TransactionTool transactionTool, ObjectMapper objectMapper,
                       ImageGenerationTool imageGenerationTool) {
        this.transactionTool = transactionTool;
        this.imageGenerationTool = imageGenerationTool;
        this.objectMapper = objectMapper;

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }
        this.model = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(MODEL)
                        .build();
        this.toolSpecifications = new ArrayList<>();
        this.toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(transactionTool));
        this.toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(imageGenerationTool));
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
                System.out.println(
                        "Tool requested: " + toolRequest.name());
                System.out.println(
                        "Tool arguments: " + toolRequest.arguments());
                String result = executeTool(toolRequest);
                // Return generated images directly to the UI.
                if (result.startsWith("IMAGE:")) {
                    return result;
                }
                messages.add(ToolExecutionResultMessage.from(toolRequest, result));
            }
        }

        throw new IllegalStateException("Maximum tool-calling rounds exceeded");
    }

    private String executeTool(ToolExecutionRequest toolRequest) {

        try {
            JsonNode arguments = objectMapper.readTree(toolRequest.arguments());
            if ("generateImage".equals(toolRequest.name()) || "generate_image".equals(toolRequest.name())) {
                JsonNode promptNode = arguments.get("prompt");
                if (promptNode == null || !promptNode.isTextual() || promptNode.asText().isBlank()) {
                    return "Invalid or missing image prompt.";
                }

                String base64Image = imageGenerationTool.generateImage(promptNode.asText());
                return "IMAGE:" + base64Image;
            }

            if ("getTotalAmount".equals(toolRequest.name()) || "get_total_amount".equals(toolRequest.name())) {
                JsonNode accountIdNode = arguments.has("account_id") ? arguments.get("account_id")
                        : arguments.get("accountId");
                if (accountIdNode == null) {
                    return "Invalid or missing account ID.";
                }

                Integer accountId = accountIdNode.isIntegralNumber() && accountIdNode.canConvertToInt()
                        ? accountIdNode.intValue() : Integer.valueOf(accountIdNode.asText());
                return transactionTool.getTotalAmount(accountId);
            }
            throw new IllegalArgumentException("Unknown tool: " + toolRequest.name());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute tool: " + toolRequest.name(), e);
        }
    }
}
