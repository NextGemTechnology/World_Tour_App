package com.example.personalassistant.controller;

import com.example.personalassistant.mongoRepository.ChatLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.personalassistant.service.AIService;
import com.example.personalassistant.dto.PromptRequestDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {


    @Autowired
    private AIService aiService;

    @Autowired(required = false)
    private com.example.personalassistant.service.MongoService mongoService;

    @PostMapping("/prompt")
    public ResponseEntity<?> handlePrompt(@RequestBody Map<String, String> req) {
        String sessionId = req.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 5);
        }

        return aiService.processPrompt(
                req.get("prompt"),
                req.getOrDefault("role", "GUEST"),
                sessionId,
                req.get("email")
        );
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        if (mongoService == null) {
            return ResponseEntity.ok(Map.of("sessionId", sessionId, "messages", List.of()));
        }
        return mongoService.getChatSession(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(null));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(@RequestParam String email) {
        if (mongoService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(mongoService.getUserChatSessions(email));
    }
}
