package com.example.personalassistant.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryService {

    private static final int MAX_TURNS = 6; // Max 6 user-AI interaction pairs
    private static final int MAX_CHAR_BUDGET = 2500; // ~600 tokens

    private final Map<String, LinkedList<String>> memory = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MongoService mongoService;

    public String getContext(String sessionId) {
        if (sessionId == null) return "";
        LinkedList<String> list = memory.get(sessionId);

        // If in-memory cache is cold (e.g. server restarted), restore from MongoDB ChatSession
        if ((list == null || list.isEmpty()) && mongoService != null) {
            try {
                var messages = mongoService.getMessagesForSession(sessionId);
                if (messages != null && !messages.isEmpty()) {
                    list = new LinkedList<>();
                    // Take up to MAX_TURNS * 2 most recent messages
                    int startIndex = Math.max(0, messages.size() - (MAX_TURNS * 2));
                    for (int i = startIndex; i < messages.size(); i++) {
                        var msg = messages.get(i);
                        String prefix = "AI".equalsIgnoreCase(msg.getSender()) ? "AI: " : "User: ";
                        list.add(prefix + (msg.getText() != null ? msg.getText().trim() : ""));
                    }
                    memory.put(sessionId, list);
                }
            } catch (Exception ignored) {}
        }

        if (list == null || list.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        // Read recent history respecting budget
        synchronized (list) {
            for (String entry : list) {
                if (sb.length() + entry.length() > MAX_CHAR_BUDGET) {
                    break;
                }
                sb.append(entry).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public void save(String sessionId, String user, String ai) {
        if (sessionId == null) return;
        LinkedList<String> list = memory.computeIfAbsent(sessionId, k -> new LinkedList<>());
        synchronized (list) {
            list.add("User: " + (user != null ? user.trim() : ""));
            list.add("AI: " + (ai != null ? ai.trim() : ""));

            // Enforce sliding window (keep at most MAX_TURNS * 2 entries)
            while (list.size() > MAX_TURNS * 2) {
                list.removeFirst();
            }
        }
    }

    public void clear(String sessionId) {
        if (sessionId != null) {
            memory.remove(sessionId);
        }
    }
}