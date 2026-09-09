package com.example.personalassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "otp_send_tracker")
public class OtpSendTracker {

    @Id
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "send_date")
    private LocalDate sendDate;

    @Column(name = "send_count")
    private int sendCount = 0;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;
}
