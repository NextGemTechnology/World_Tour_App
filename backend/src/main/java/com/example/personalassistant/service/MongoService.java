package com.example.personalassistant.service;

import com.example.personalassistant.mongo.ChatLog;
import com.example.personalassistant.mongo.ChatMessage;
import com.example.personalassistant.mongo.ChatSession;
import com.example.personalassistant.mongoRepository.ChatLogRepository;
import com.example.personalassistant.mongoRepository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MongoService {

    private static final Logger log = LoggerFactory.getLogger(MongoService.class);

    @Autowired(required = false)
    private ChatLogRepository repo;

    @Autowired(required = false)
    private ChatSessionRepository sessionRepo;

    /**
     * Saves user prompt and AI response to the single ChatSession document identified by sessionId.
     * Appends to messages array instead of creating a new ObjectId document for every turn.
     */
    public void saveChat(
            String sessionId,
            String email,
            String userPrompt,
            String aiResponse,
            String intent
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Primary: Save to single ChatSession document per session (1 session = 1 ObjectId)
        if (sessionRepo != null) {
            try {
                ChatSession session = sessionRepo.findBySessionId(sessionId).orElse(null);

                if (session == null) {
                    session = new ChatSession();
                    session.setSessionId(sessionId);
                    session.setEmail(email);
                    if (userPrompt != null && !userPrompt.isBlank()) {
                        String title = userPrompt.trim();
                        session.setTitle(title.length() > 40 ? title.substring(0, 40) + "..." : title);
                    }
                    session.setCreatedAt(now);
                    session.setUpdatedAt(now);
                    session.setMessages(new ArrayList<>());
                } else {
                    session.setUpdatedAt(now);
                    if ((session.getEmail() == null || session.getEmail().isBlank()) && email != null) {
                        session.setEmail(email);
                    }
                    if (session.getMessages() == null) {
                        session.setMessages(new ArrayList<>());
                    }
                }

                if (userPrompt != null && !userPrompt.isBlank()) {
                    session.getMessages().add(new ChatMessage("USER", userPrompt.trim(), null, now));
                }
                if (aiResponse != null && !aiResponse.isBlank()) {
                    session.getMessages().add(new ChatMessage("AI", aiResponse.trim(), intent, now));
                }

                sessionRepo.save(session);
                return;
            } catch (Exception e) {
                log.warn("Failed to persist to ChatSession in MongoDB: {}", e.getMessage());
            }
        }

        // 2. Fallback: Save to legacy ChatLog if sessionRepo is unavailable
        if (repo != null) {
            try {
                ChatLog chatLog = new ChatLog();
                chatLog.setSessionId(sessionId);
                chatLog.setEmail(email);
                chatLog.setUserPrompt(userPrompt);
                chatLog.setAiResponse(aiResponse);
                chatLog.setIntent(intent);
                chatLog.setCreatedAt(now);
                repo.save(chatLog);
            } catch (Exception e) {
                log.warn("Failed to persist to ChatLog: {}", e.getMessage());
            }
        }
    }

    public void saveAiChat(String userPrompt) {
        saveChat("default", null, userPrompt, null, "CHAT");
    }

    public void saveAiChat(String sessionId, String email, String userPrompt, String aiResponse) {
        saveChat(sessionId, email, userPrompt, aiResponse, "CHAT");
    }

    public Optional<ChatSession> getChatSession(String sessionId) {
        if (sessionRepo == null || sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        try {
            return sessionRepo.findBySessionId(sessionId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<ChatMessage> getMessagesForSession(String sessionId) {
        return getChatSession(sessionId)
                .map(ChatSession::getMessages)
                .orElse(List.of());
    }

    public List<ChatSession> getUserChatSessions(String email) {
        if (sessionRepo == null || email == null || email.isBlank()) {
            return List.of();
        }
        try {
            return sessionRepo.findByEmailOrderByUpdatedAtDesc(email);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<String> getUserPrompt() {
        if (sessionRepo != null) {
            try {
                List<ChatSession> sessions = sessionRepo.findAll();
                return sessions.stream()
                        .filter(s -> s.getMessages() != null)
                        .flatMap(s -> s.getMessages().stream())
                        .filter(m -> "USER".equalsIgnoreCase(m.getSender()) && m.getText() != null && !m.getText().isBlank())
                        .map(ChatMessage::getText)
                        .toList();
            } catch (Exception ignored) {}
        }

        if (repo == null) return List.of();
        try {
            List<ChatLog> logs = repo.findAllByOrderByCreatedAtDesc();
            return logs.stream()
                    .map(ChatLog::getUserPrompt)
                    .filter(prompt -> prompt != null && !prompt.isBlank())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
