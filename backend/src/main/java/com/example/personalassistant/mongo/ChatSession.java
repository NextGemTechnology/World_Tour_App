package com.example.personalassistant.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ai_chat_sessions")
public class ChatSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    private String email;

    private String title;

    private List<ChatMessage> messages = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
