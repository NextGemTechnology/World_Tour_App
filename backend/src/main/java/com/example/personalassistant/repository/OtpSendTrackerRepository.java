package com.example.personalassistant.repository;

import com.example.personalassistant.entity.OtpSendTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpSendTrackerRepository extends JpaRepository<OtpSendTracker, String> {
}
