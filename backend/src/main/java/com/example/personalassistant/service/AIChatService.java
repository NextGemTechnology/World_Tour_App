package com.example.personalassistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIChatService {

    @Autowired
    private MongoService mongoService;

    @Autowired
    private com.example.personalassistant.service.ai.AIModelService aiModelService;

    public String getAIResponse(String userMessage) {
        return getAIResponse(userMessage, null, null);
    }

    public String getAIResponse(String userMessage, String sessionId, String email) {
        try {
            String answer = aiModelService.generate(
                    "You are LuxNes AI Travel Assistant. Provide helpful, conversational responses about destinations, trips, and reservations.",
                    userMessage
            );
            if (answer == null || answer.isBlank()) {
                answer = "Hello! I am your AI Travel Assistant. How can I help you plan your travel or stay today?";
            }
            String effSessionId = (sessionId != null && !sessionId.isBlank()) ? sessionId : "default";
            mongoService.saveAiChat(effSessionId, email, userMessage, answer);
            return answer;
        } catch (Exception e) {
            return "I am currently assisting many travelers. Please let me know where you'd like to travel!";
        }
    }
}