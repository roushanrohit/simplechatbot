package com.chatbot.simplechatbot.tool;

import dev.langchain4j.model.openai.OpenAiImageModel;
import org.springframework.stereotype.Component;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

@Component
public class ImageGenerationTool {

    private final OpenAiImageModel imageModel;

    public ImageGenerationTool() {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }

        this.imageModel = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-image-1.5")
                .build();
    }

    @Tool("Generate an image based on the user's description")
    public String generateImage(@P("Detailed description of the image to generate") String prompt) {
        return imageModel.generate(prompt)
                .content()
                .base64Data();
    }
}