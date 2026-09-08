package com.example.personalassistant;

import com.example.personalassistant.mongo.ChatMessage;
import com.example.personalassistant.mongo.ChatSession;
import com.example.personalassistant.mongoRepository.ChatSessionRepository;
import com.example.personalassistant.service.ChatMemoryService;
import com.example.personalassistant.service.MongoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class MongoChatSessionTest {

    private FakeChatSessionRepository sessionRepo;
    private MongoService mongoService;
    private ChatMemoryService chatMemoryService;

    @BeforeEach
    void setUp() {
        sessionRepo = new FakeChatSessionRepository();
        mongoService = new MongoService();
        ReflectionTestUtils.setField(mongoService, "sessionRepo", sessionRepo);

        chatMemoryService = new ChatMemoryService();
        ReflectionTestUtils.setField(chatMemoryService, "mongoService", mongoService);
    }

    @Test
    void testFirstTurn_CreatesSingleChatSessionDocument() {
        String sessionId = "sess_user_001";
        String email = "test@gmail.com";

        mongoService.saveChat(sessionId, email, "hello", "Planning your trip to AI!", "PLAN_TRIP");

        assertEquals(1, sessionRepo.savedSessions.size(), "Only 1 ChatSession document must be created!");
        ChatSession saved = sessionRepo.savedSessions.get(0);
        assertEquals(sessionId, saved.getSessionId());
        assertEquals(email, saved.getEmail());
        assertNotNull(saved.getMessages());
        assertEquals(2, saved.getMessages().size(), "First turn must store exactly 2 messages (USER + AI)!");
        assertEquals("USER", saved.getMessages().get(0).getSender());
        assertEquals("hello", saved.getMessages().get(0).getText());
        assertEquals("AI", saved.getMessages().get(1).getSender());
        assertEquals("Planning your trip to AI!", saved.getMessages().get(1).getText());
    }

    @Test
    void testSecondTurn_UpdatesExistingChatSessionDocument_NoNewDocument() {
        String sessionId = "sess_user_002";
        String email = "test@gmail.com";

        ChatSession existing = new ChatSession();
        existing.setId("6a9a45305a25a93da405e82c"); // Fixed single ObjectId
        existing.setSessionId(sessionId);
        existing.setEmail(email);
        existing.setMessages(new ArrayList<>(List.of(
                new ChatMessage("USER", "hello", null, LocalDateTime.now().minusMinutes(2)),
                new ChatMessage("AI", "Planning your trip to AI!", "PLAN_TRIP", LocalDateTime.now().minusMinutes(2))
        )));
        sessionRepo.database.put(sessionId, existing);

        // User sends 2nd turn with same sessionId
        mongoService.saveChat(sessionId, email, "i want to go delhi tomorrow", "Here are flights and trains to Delhi", "PLAN_TRIP");

        // Verify still only 1 document in database
        assertEquals(1, sessionRepo.database.size(), "Database must still contain only 1 document!");
        ChatSession updated = sessionRepo.database.get(sessionId);
        assertEquals("6a9a45305a25a93da405e82c", updated.getId(), "Must keep the exact same single ObjectId!");
        assertEquals(4, updated.getMessages().size(), "Must append messages into the same document!");
        assertEquals("i want to go delhi tomorrow", updated.getMessages().get(2).getText());
        assertEquals("Here are flights and trains to Delhi", updated.getMessages().get(3).getText());
    }

    @Test
    void testChatMemoryService_RestoresContextFromChatSessionDocument() {
        String sessionId = "sess_user_003";
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setMessages(List.of(
                new ChatMessage("USER", "I want to visit Jaipur", null, LocalDateTime.now().minusMinutes(5)),
                new ChatMessage("AI", "Jaipur is beautiful! When would you like to travel?", "CHAT", LocalDateTime.now().minusMinutes(4))
        ));
        sessionRepo.database.put(sessionId, session);

        // When ChatMemoryService has empty memory cache, it should load from ChatSession in MongoDB
        String context = chatMemoryService.getContext(sessionId);
        assertTrue(context.contains("User: I want to visit Jaipur"), "Context should include prior user prompt from Mongo!");
        assertTrue(context.contains("AI: Jaipur is beautiful!"), "Context should include prior AI response from Mongo!");
    }

    // In-memory fake repository implementation for fast, reliable testing
    static class FakeChatSessionRepository implements ChatSessionRepository {
        final Map<String, ChatSession> database = new HashMap<>();
        final List<ChatSession> savedSessions = new ArrayList<>();

        @Override
        public Optional<ChatSession> findBySessionId(String sessionId) {
            return Optional.ofNullable(database.get(sessionId));
        }

        @Override
        public List<ChatSession> findByEmailOrderByUpdatedAtDesc(String email) {
            return database.values().stream()
                    .filter(s -> Objects.equals(s.getEmail(), email))
                    .toList();
        }

        @Override
        public void deleteBySessionId(String sessionId) {
            database.remove(sessionId);
        }

        @Override
        public <S extends ChatSession> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
            }
            database.put(entity.getSessionId(), entity);
            savedSessions.add(entity);
            return entity;
        }

        @Override public <S extends ChatSession> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<ChatSession> findById(String s) { return Optional.empty(); }
        @Override public boolean existsById(String s) { return false; }
        @Override public List<ChatSession> findAll() { return new ArrayList<>(database.values()); }
        @Override public List<ChatSession> findAllById(Iterable<String> strings) { return List.of(); }
        @Override public long count() { return database.size(); }
        @Override public void deleteById(String s) {}
        @Override public void delete(ChatSession entity) { database.remove(entity.getSessionId()); }
        @Override public void deleteAllById(Iterable<? extends String> strings) {}
        @Override public void deleteAll(Iterable<? extends ChatSession> entities) {}
        @Override public void deleteAll() { database.clear(); }
        @Override public List<ChatSession> findAll(Sort sort) { return List.of(); }
        @Override public Page<ChatSession> findAll(Pageable pageable) { return Page.empty(); }
        @Override public <S extends ChatSession> S insert(S entity) { return save(entity); }
        @Override public <S extends ChatSession> List<S> insert(Iterable<S> entities) { return List.of(); }
        @Override public <S extends ChatSession> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends ChatSession> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends ChatSession> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends ChatSession> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends ChatSession> long count(Example<S> example) { return 0; }
        @Override public <S extends ChatSession> boolean exists(Example<S> example) { return false; }
        @Override public <S extends ChatSession, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }
}
