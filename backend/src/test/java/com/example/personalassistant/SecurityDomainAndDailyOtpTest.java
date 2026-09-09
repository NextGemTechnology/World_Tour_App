package com.example.personalassistant;

import com.example.personalassistant.dto.Response;
import com.example.personalassistant.dto.UserDto;
import com.example.personalassistant.repository.OtpRepository;
import com.example.personalassistant.repository.UserLoginRepository;
import com.example.personalassistant.repository.UserRepository;
import com.example.personalassistant.service.EmailService;
import com.example.personalassistant.service.OtpService;
import com.example.personalassistant.service.UserService;
import com.example.personalassistant.util.DomainValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityDomainAndDailyOtpTest {

    private OtpService otpService;
    private UserService userService;

    @BeforeEach
    public void setup() {
        otpService = new OtpService();
        userService = new UserService();

        OtpRepository otpRepoProxy = (OtpRepository) Proxy.newProxyInstance(
                OtpRepository.class.getClassLoader(),
                new Class<?>[]{OtpRepository.class},
                (proxy, method, args) -> null
        );

        UserLoginRepository loginRepoProxy = (UserLoginRepository) Proxy.newProxyInstance(
                UserLoginRepository.class.getClassLoader(),
                new Class<?>[]{UserLoginRepository.class},
                (proxy, method, args) -> {
                    if ("existsByEmail".equals(method.getName())) return false;
                    if ("findByEmail".equals(method.getName())) return Optional.empty();
                    return null;
                }
        );

        UserRepository userRepoProxy = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("existsByEmail".equals(method.getName())) return false;
                    if ("findByMobile".equals(method.getName())) return Optional.empty();
                    return null;
                }
        );

        EmailService dummyEmail = new EmailService() {
            @Override
            public void sendOtp(String toEmail, String otp) {}
            @Override
            public void sendNewPassword(String toEmail, String newPassword) {}
        };

        ReflectionTestUtils.setField(otpService, "otpRepository", otpRepoProxy);
        ReflectionTestUtils.setField(otpService, "emailService", dummyEmail);

        ReflectionTestUtils.setField(userService, "otpService", otpService);
        ReflectionTestUtils.setField(userService, "otpRepository", otpRepoProxy);
        ReflectionTestUtils.setField(userService, "userLoginRepository", loginRepoProxy);
        ReflectionTestUtils.setField(userService, "userRepository", userRepoProxy);
        ReflectionTestUtils.setField(userService, "emailService", dummyEmail);
    }

    @Test
    public void testDomainValidatorGenuineAndDisposableDomains() {
        // Genuine Domains
        assertTrue(DomainValidator.isGenuineDomain("alice@gmail.com"));
        assertTrue(DomainValidator.isGenuineDomain("bob@yahoo.com"));
        assertTrue(DomainValidator.isGenuineDomain("carol@outlook.com"));
        assertTrue(DomainValidator.isGenuineDomain("dan@hotmail.com"));
        assertTrue(DomainValidator.isGenuineDomain("eve@icloud.com"));
        assertTrue(DomainValidator.isGenuineDomain("frank@proton.me"));
        assertTrue(DomainValidator.isGenuineDomain("grace@zoho.com"));

        // Fake / Disposable Domains
        assertFalse(DomainValidator.isGenuineDomain("spammer@mailinator.com"));
        assertFalse(DomainValidator.isGenuineDomain("hacker@tempmail.com"));
        assertFalse(DomainValidator.isGenuineDomain("bot@10minutemail.com"));
        assertFalse(DomainValidator.isGenuineDomain("fraud@sharklasers.com"));
        assertFalse(DomainValidator.isGenuineDomain("burner@yopmail.com"));
        assertFalse(DomainValidator.isGenuineDomain("invalid-email-no-at-sign"));
        assertFalse(DomainValidator.isGenuineDomain(""));
        assertFalse(DomainValidator.isGenuineDomain(null));
    }

    @Test
    public void testDailyOtpLimitCappedAtThreeSendsPerDay() {
        String testEmail = "dailylimituser@gmail.com";

        // Send 1 - Allowed
        String otp1 = otpService.generateAndSendOtp(testEmail);
        assertNotNull(otp1);
        assertEquals(2, otpService.getRemainingDailyOtpSends(testEmail));
        assertFalse(otpService.isDailyLimitExceeded(testEmail));

        // Send 2 - Allowed
        String otp2 = otpService.generateAndSendOtp(testEmail);
        assertNotNull(otp2);
        assertEquals(1, otpService.getRemainingDailyOtpSends(testEmail));
        assertFalse(otpService.isDailyLimitExceeded(testEmail));

        // Send 3 - Allowed
        String otp3 = otpService.generateAndSendOtp(testEmail);
        assertNotNull(otp3);
        assertEquals(0, otpService.getRemainingDailyOtpSends(testEmail));
        assertTrue(otpService.isDailyLimitExceeded(testEmail));

        // Send 4 - Blocked by daily rate limit!
        String otp4 = otpService.generateAndSendOtp(testEmail);
        assertNull(otp4, "4th OTP attempt on the same day must be rejected");

        // When requesting via UserService, returns 429 TOO_MANY_REQUESTS
        UserDto dto = new UserDto();
        dto.setEmail(testEmail);
        ResponseEntity<Response> res = userService.sendRegistrationOtp(dto);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, res.getStatusCode());
        assertTrue(res.getBody().getMessage().contains("Daily OTP limit reached"));
    }
}
