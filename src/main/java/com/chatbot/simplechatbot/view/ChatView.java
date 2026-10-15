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
        input.addKeyPressListener(
                com.vaadin.flow.component.Key.ENTER,
                event -> sendMessage()
        );

        add(title, conversation, input, sendButton);
        expand(conversation);
    }

    private void sendMessage() {

        String userText = input.getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        // Display the user's message
        conversation.add(new Paragraph("You: " + userText));

        // Add the user message to conversation history
        history.add(UserMessage.from(userText));

        input.clear();

        try {
            // Send the complete conversation history
            String answer = chatService.chat(history);

            // Save the assistant's response for future turns
            history.add(AiMessage.from(answer));

            // Display the assistant's response
            conversation.add(new Paragraph("Assistant: " + answer));

        } catch (Exception e) {
            conversation.add(new Paragraph("Sorry, something went wrong: " + e.getMessage()));
        }
    }
}
