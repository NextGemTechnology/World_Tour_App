package com.example.personalassistant.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String sender; // "USER" or "AI"
    private String text;
    private String intent;
    private LocalDateTime timestamp;
}
