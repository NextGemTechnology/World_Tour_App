package com.example.personalassistant;

import com.example.personalassistant.dto.Response;
import com.example.personalassistant.entity.User;
import com.example.personalassistant.entity.UserLogin;
import com.example.personalassistant.repository.UserLoginRepository;
import com.example.personalassistant.repository.UserRepository;
import com.example.personalassistant.security.JwtUtil;
import com.example.personalassistant.service.OtpService;
import com.example.personalassistant.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthLockoutAndTwoFactorTest {

    private UserService userService;
    private UserLogin testLogin;
    private User testUser;
    private boolean otpVerifiedResult = true;

    @BeforeEach
    public void setup() {
        userService = new UserService();

        testLogin = new UserLogin();
        testLogin.setEmail("test@gmail.com");
        testLogin.setPassword("$2a$10$encodedPassword123");
        testLogin.setFailedLoginAttempts(0);
        testLogin.setAccountLocked(false);
        testLogin.setTwoFactorEnabled(true);

        testUser = new User();
        testUser.setEmail("test@gmail.com");
        testUser.setResetToken("reset-token-123");
        testUser.setTokenExpiry(LocalDateTime.now().plusMinutes(10));

        // Proxy UserLoginRepository
        UserLoginRepository loginRepoProxy = (UserLoginRepository) Proxy.newProxyInstance(
                UserLoginRepository.class.getClassLoader(),
                new Class<?>[]{UserLoginRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByEmail") || method.getName().equals("findById")) {
                        if (args != null && args.length > 0 && "test@gmail.com".equals(args[0])) {
                            return Optional.of(testLogin);
                        }
                        return Optional.empty();
                    }
                    if (method.getName().equals("save")) {
                        return args[0];
                    }
                    return null;
                }
        );

        // Proxy UserRepository
        UserRepository userRepoProxy = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByResetToken")) {
                        if (args != null && args.length > 0 && "reset-token-123".equals(args[0])) {
                            return testUser;
                        }
                        return null;
                    }
                    if (method.getName().equals("save")) {
                        return args[0];
                    }
                    return null;
                }
        );

        // Simple PasswordEncoder
        PasswordEncoder encoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "enc:" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return "correctPassword".equals(rawPassword);
            }
        };

        // Dummy OtpService
        OtpService otpService = new OtpService() {
            @Override
            public com.example.personalassistant.entity.OtpVerification sendOtp(String email) {
                return null;
            }

            @Override
            public String generateAndSendOtp(String email) {
                return "123456";
            }

            @Override
            public boolean verifyOtp(String email, String otp) {
                return otpVerifiedResult && "123456".equals(otp);
            }
        };

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKey1234567890123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);

        ReflectionTestUtils.setField(userService, "userLoginRepository", loginRepoProxy);
        ReflectionTestUtils.setField(userService, "userRepository", userRepoProxy);
        ReflectionTestUtils.setField(userService, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(userService, "otpService", otpService);
        ReflectionTestUtils.setField(userService, "jwtUtil", jwtUtil);
    }

    @Test
    public void testFailedLoginIncrementsAttempts() {
        ResponseEntity<Response> res = userService.loginUser("test@gmail.com", "wrongPassword");
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals(1, testLogin.getFailedLoginAttempts());
        assertFalse(testLogin.isAccountLocked());
    }

    @Test
    public void testAccountLockoutAfterThreeAttempts() {
        // Attempts 1 and 2
        userService.loginUser("test@gmail.com", "wrongPassword");
        userService.loginUser("test@gmail.com", "wrongPassword");

        // Attempt 3 triggers lockout
        ResponseEntity<Response> res = userService.loginUser("test@gmail.com", "wrongPassword");
        assertEquals(HttpStatus.LOCKED, res.getStatusCode());
        assertTrue(testLogin.isAccountLocked(), "Account must be locked on 3rd failed attempt");

        // Attempt 4 is blocked immediately with 423
        ResponseEntity<Response> res4 = userService.loginUser("test@gmail.com", "correctPassword");
        assertEquals(HttpStatus.LOCKED, res4.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSuccessfulLoginTriggersTwoFactor() {
        ResponseEntity<Response> res = userService.loginUser("test@gmail.com", "correctPassword");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().getData() instanceof Map);

        Map<String, Object> data = (Map<String, Object>) res.getBody().getData();
        assertEquals(true, data.get("requires2fa"));
        assertEquals("test@gmail.com", data.get("email"));
    }

    @Test
    public void testVerifyTwoFactorSuccess() {
        ResponseEntity<Response> res = userService.verify2fa("test@gmail.com", "123456");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody().getData());
        assertTrue(res.getBody().getData() instanceof String, "JWT token string expected");
    }

    @Test
    public void testResetPasswordUnlocksAccount() {
        testLogin.setAccountLocked(true);
        testLogin.setFailedLoginAttempts(4);

        ResponseEntity<?> res = userService.resetPassword("reset-token-123", "newSecretPassword123");
        assertEquals(200, res.getStatusCode().value());
        assertFalse(testLogin.isAccountLocked(), "Password reset must unlock the account");
        assertEquals(0, testLogin.getFailedLoginAttempts(), "Failed attempts counter must be reset to 0");
    }
}
