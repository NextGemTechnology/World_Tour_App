package com.example.personalassistant.controller;

import com.example.personalassistant.dto.*;
import com.example.personalassistant.service.AIChatService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AIChatService aiChatService;

    public ChatController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Response> ask(@RequestBody ChatRequest request) {

        Response res = new Response();

        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String answer = aiChatService.getAIResponse(request.getMessage(), sessionId, request.getEmail());

        Map<String, String> data = new HashMap<>();
        data.put("answer", answer);
        data.put("sessionId", sessionId);

        res.setData(data);

        return ResponseEntity.ok(res); // ✅ always 200
    }
}