package com.example.personalassistant.mongoRepository;

import com.example.personalassistant.mongo.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    Optional<ChatSession> findBySessionId(String sessionId);

    List<ChatSession> findByEmailOrderByUpdatedAtDesc(String email);

    void deleteBySessionId(String sessionId);
}
