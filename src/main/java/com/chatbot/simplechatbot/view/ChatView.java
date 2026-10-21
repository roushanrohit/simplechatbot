package com.chatbot.simplechatbot.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;

import com.vaadin.flow.component.html.Image;
import com.chatbot.simplechatbot.service.ChatService;

import java.util.ArrayList;
import java.util.List;

@Route("")
public class ChatView extends VerticalLayout {

    private final ChatService chatService;

    // Conversation history for this UI instance
    private final List<ChatMessage> history = new ArrayList<>();

    private final VerticalLayout conversation = new VerticalLayout();

    private final TextField input = new TextField();

    public ChatView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("AI Chatbot");

        conversation.setWidthFull();
        conversation.setPadding(true);
        conversation.setSpacing(true);

        input.setPlaceholder("Type your message...");
        input.setWidthFull();

        Button sendButton = new Button("Send");

        sendButton.addClickListener(event -> sendMessage());

        // Pressing Enter also sends the message
        input.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, event -> sendMessage());

        add(title, conversation, input, sendButton);
        expand(conversation);
    }


    private void sendMessage() {

        String userText = input.getValue().trim();
        if (userText.isEmpty()) return;

        conversation.add(new Paragraph("You: " + userText));
        history.add(UserMessage.from(userText));
        input.clear();

        try {
            String answer = chatService.chat(history);
            if (answer != null && answer.startsWith("IMAGE:")) {
                // Extract Base64 image data
                String base64 = answer.substring("IMAGE:".length());
                // Create a resource for Vaadin
                Image image = new Image("data:image/png;base64," + base64, "AI-generated image");
                // Display the generated image
                image.setMaxWidth("512px");
                image.setWidth("100%");
                conversation.add(new Paragraph("Assistant: Here is your generated image:"), image);

                // Don't store raw Base64 data in chat history
                history.add(AiMessage.from("I generated an image based on your request."));

            } else {

                // Normal text response
                history.add(AiMessage.from(answer));
                conversation.add(new Paragraph("Assistant: " + answer));
            }

        } catch (Exception e) {
            e.printStackTrace();
            conversation.add(new Paragraph("Sorry, something went wrong: " + e.getMessage()));
        }
    }

}
