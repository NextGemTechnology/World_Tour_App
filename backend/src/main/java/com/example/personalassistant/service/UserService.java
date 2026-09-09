package com.example.personalassistant.service;

import java.util.UUID;
import java.time.LocalDateTime;

import com.example.personalassistant.dto.OtpVerificationResult;
import com.example.personalassistant.entity.OtpVerification;
import com.example.personalassistant.enums.AccountEnum;
import com.example.personalassistant.repository.OtpRepository;
import com.example.personalassistant.util.DomainValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.personalassistant.entity.User;
import com.example.personalassistant.dto.UserDto;
import com.example.personalassistant.dto.Response;
import com.example.personalassistant.security.JwtUtil;
import com.example.personalassistant.dto.ErrorDetails;
import com.example.personalassistant.entity.UserLogin;
import com.example.personalassistant.dto.ChangePasswordDto;
import com.example.personalassistant.repository.UserRepository;
import com.example.personalassistant.repository.UserLoginRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;



@Slf4j
@Service
//@Transactional  WIP
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired(required = false)
    private com.example.personalassistant.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    public ResponseEntity<Response> sendRegistrationOtp(UserDto dto) {
        Response response = new Response();

        if (dto.getEmail() == null || !DomainValidator.isGenuineDomain(dto.getEmail())) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "Only genuine email domains (e.g., Gmail, Yahoo, Outlook, iCloud) are allowed!"
            );
            response.setError(error);
            response.setMessage("Only genuine email domains (e.g., Gmail, Yahoo, Outlook, iCloud) are allowed!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        if (otpService.isDailyLimitExceeded(normalizedEmail)) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Daily OTP limit reached. You can only request up to 3 verification codes per day. Please try again tomorrow."
            );
            response.setError(error);
            response.setMessage("Daily OTP limit reached. You can only request up to 3 verification codes per day. Please try again tomorrow.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        if (userLoginRepository.existsByEmail(normalizedEmail) ||
            userLoginRepository.existsByEmail(dto.getEmail().trim()) ||
            userRepository.existsByEmail(normalizedEmail) ||
            userRepository.existsByEmail(dto.getEmail().trim())) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "User already exists try with another email"
            );
            response.setError(error);
            response.setMessage("User already exists try with another email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (dto.getMobile() != null && !dto.getMobile().isBlank()) {
            String cleanMobile = dto.getMobile().replaceAll("[^0-9]", "");
            if (cleanMobile.length() >= 10) {
                String last10 = cleanMobile.substring(cleanMobile.length() - 10);
                if (userRepository.findByMobile(last10).isPresent()) {
                    ErrorDetails error = new ErrorDetails(
                            HttpStatus.BAD_REQUEST,
                            "Mobile number is already registered with another account."
                    );
                    response.setError(error);
                    response.setMessage("Mobile number is already registered with another account.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
        }

        String generatedOtp = otpService.generateAndSendOtp(normalizedEmail);

        response.setSuccess(true);
        response.setMessage("Verification OTP sent to " + dto.getEmail());
        response.setData(java.util.Map.of(
                "otp", generatedOtp != null ? generatedOtp : "",
                "email", normalizedEmail
        ));
        return ResponseEntity.ok(response);
    }

    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Response> registerUser(UserDto dto) {
        Response response = new Response();

        if (dto.getEmail() == null || !DomainValidator.isGenuineDomain(dto.getEmail())) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "Only genuine email domains (e.g., Gmail, Yahoo, Outlook, iCloud) are allowed!"
            );
            response.setError(error);
            response.setMessage("Only genuine email domains (e.g., Gmail, Yahoo, Outlook, iCloud) are allowed!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        if (userLoginRepository.existsByEmail(normalizedEmail) ||
            userLoginRepository.existsByEmail(dto.getEmail().trim()) ||
            userRepository.existsByEmail(normalizedEmail) ||
            userRepository.existsByEmail(dto.getEmail().trim())) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "User already exists try with another email"
            );
            response.setError(error);
            response.setMessage("User already exists try with another email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (dto.getMobile() != null && !dto.getMobile().isBlank()) {
            String cleanMobile = dto.getMobile().replaceAll("[^0-9]", "");
            if (cleanMobile.length() >= 10) {
                String last10 = cleanMobile.substring(cleanMobile.length() - 10);
                if (userRepository.findByMobile(last10).isPresent()) {
                    ErrorDetails error = new ErrorDetails(
                            HttpStatus.BAD_REQUEST,
                            "Mobile number is already registered with another account."
                    );
                    response.setError(error);
                    response.setMessage("Mobile number is already registered with another account.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
        }

        // Enforce OTP verification requirement
        if (dto.getOtp() == null || dto.getOtp().trim().isBlank()) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    "Verification OTP code is required to complete registration."
            );
            response.setError(error);
            response.setMessage("Verification OTP code is required to complete registration.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Verify registration OTP with 3-attempt brute force check
        OtpVerificationResult otpResult = otpService.verifyOtpWithResult(normalizedEmail, dto.getOtp().trim());
        if (!otpResult.isSuccess()) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.BAD_REQUEST,
                    otpResult.getMessage()
            );
            response.setError(error);
            response.setMessage(otpResult.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Clean mobile number
        String userMobile = dto.getMobile() != null ? dto.getMobile().replaceAll("[^0-9]", "") : "";
        if (userMobile.length() > 10) {
            userMobile = userMobile.substring(userMobile.length() - 10);
        }

        // Save User Entity
        User user = new User();
        user.setName(dto.getName() != null ? dto.getName().trim() : "Traveler");
        user.setEmail(normalizedEmail);
        user.setMobile(userMobile.isBlank() ? dto.getMobile() : userMobile);
        user.setAccountEnum(AccountEnum.ACTIVE);
        User savedUser = userRepository.save(user);

        // Save Credentials Entity
        UserLogin login = new UserLogin();
        login.setEmail(normalizedEmail);
        login.setPassword(passwordEncoder.encode(dto.getPassword()));
        login.setEmailVerified(true);
        login.setTwoFactorEnabled(false);
        login.setAccountLocked(false);
        login.setFailedLoginAttempts(0);
        userLoginRepository.save(login);

        // Delete used OTP
        try {
            otpRepository.deleteByEmail(normalizedEmail);
            otpRepository.deleteByEmail(dto.getEmail().trim());
        } catch (Exception ignored) {}

        response.setSuccess(true);
        response.setData(savedUser);
        response.setMessage("register successful");
        return ResponseEntity.ok(response);
    }


    public ResponseEntity<Response> loginUser(String email, String password) {
        Response response = new Response();
        UserLogin user = userLoginRepository.findByEmail(email).orElse(null);
        if (user == null) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.NOT_FOUND,
                    "User does not exist"
            );
            response.setError(error);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 🔒 CHECK LOCKOUT (3 failed attempts)
        if (user.isAccountLocked()) {
            ErrorDetails error = new ErrorDetails(
                    HttpStatus.LOCKED,
                    "Account locked due to 3 consecutive failed password attempts. Please use password retrieval (Forgot Password) to unlock your account."
            );
            response.setError(error);
            return ResponseEntity.status(HttpStatus.LOCKED).body(response);
        }

        // CHECK PASSWORD
        if (!passwordEncoder.matches(password, user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 3) {
                user.setAccountLocked(true);
                userLoginRepository.save(user);
                ErrorDetails error = new ErrorDetails(
                        HttpStatus.LOCKED,
                        "Account locked due to 3 consecutive failed password attempts. Please use password retrieval (Forgot Password) to unlock your account."
                );
                response.setError(error);
                return ResponseEntity.status(HttpStatus.LOCKED).body(response);
            } else {
                userLoginRepository.save(user);
                int remaining = 3 - attempts;
                ErrorDetails error = new ErrorDetails(
                        HttpStatus.BAD_REQUEST,
                        "Incorrect password. You have " + remaining + " attempt(s) remaining before account lockout."
                );
                response.setError(error);
                return ResponseEntity.badRequest().body(response);
            }
        }

        // Password matches -> reset attempts only if there were prior failed attempts
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userLoginRepository.save(user);
        }

        // 🔐 2FA LOGIN: Send OTP if enabled
        if (user.isTwoFactorEnabled()) {
            String otp = otpService.generateAndSendOtp(email);
            user.setTwoFactorOtp(otp);
            user.setTwoFactorOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));
            userLoginRepository.save(user);

            java.util.Map<String, Object> twoFactorData = new java.util.HashMap<>();
            twoFactorData.put("requires2fa", true);
            twoFactorData.put("email", email);
            twoFactorData.put("devOtp", otp);
            twoFactorData.put("message", "2FA OTP sent to your registered email. (Local Code: " + otp + ")");

            response.setData(twoFactorData);
            return ResponseEntity.ok(response);
        }

        String token = jwtUtil.generateToken(email, "User");
        response.setData(token);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Response> verify2fa(String email, String otp) {
        Response response = new Response();
        UserLogin user = userLoginRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.setError(new ErrorDetails(HttpStatus.NOT_FOUND, "User not found"));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (user.isAccountLocked()) {
            response.setError(new ErrorDetails(HttpStatus.LOCKED, "Account is locked. Please reset password."));
            return ResponseEntity.status(HttpStatus.LOCKED).body(response);
        }

        boolean verified = otpService.verifyOtp(email, otp);
        if (!verified && user.getTwoFactorOtp() != null && user.getTwoFactorOtp().equals(otp != null ? otp.trim() : "")) {
            if (user.getTwoFactorOtpExpiry() != null && user.getTwoFactorOtpExpiry().isAfter(java.time.LocalDateTime.now())) {
                verified = true;
            }
        }

        if (!verified) {
            response.setError(new ErrorDetails(HttpStatus.BAD_REQUEST, "Invalid or expired OTP"));
            return ResponseEntity.badRequest().body(response);
        }

        user.setFailedLoginAttempts(0);
        user.setTwoFactorOtp(null);
        userLoginRepository.save(user);

        String token = jwtUtil.generateToken(email, "User");
        response.setData(token);
        return ResponseEntity.ok(response);
    }

    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", "Incorrect Details"));
        }

        String targetEmail = email.trim();
        String normalizedEmail = targetEmail.toLowerCase();

        UserLogin userLogin = userLoginRepository.findByEmail(targetEmail)
                .or(() -> userLoginRepository.findByEmail(normalizedEmail))
                .orElse(null);
        User user = userRepository.findByEmail(targetEmail)
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .orElse(null);

        // If not found by email and input is a 10-digit mobile number, attempt lookup by mobile
        if (user == null && targetEmail.matches("^[0-9]{10}$")) {
            user = userRepository.findByMobile(targetEmail).orElse(null);
            if (user != null && user.getEmail() != null) {
                final String foundEmail = user.getEmail();
                userLogin = userLoginRepository.findByEmail(foundEmail)
                        .or(() -> userLoginRepository.findByEmail(foundEmail.toLowerCase()))
                        .orElse(null);
            }
        }

        if (userLogin == null && user == null) {
            // Requirement 1: If not registered, return "Incorrect Details"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", "Incorrect Details"));
        }

        // Generate a new secure random 8-character alphanumeric temporary password
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String newPassword = sb.toString();

        String destinationEmail = userLogin != null ? userLogin.getEmail() : user.getEmail();

        if (userLogin == null) {
            userLogin = new UserLogin();
            userLogin.setEmail(destinationEmail);
            userLogin.setEmailVerified(true);
            userLogin.setTwoFactorEnabled(false);
        }

        userLogin.setPassword(passwordEncoder.encode(newPassword));
        userLogin.setAccountLocked(false);
        userLogin.setFailedLoginAttempts(0);
        userLoginRepository.save(userLogin);

        String resetToken = java.util.UUID.randomUUID().toString();
        if (user != null) {
            user.setResetToken(resetToken);
            user.setTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
        }

        if (passwordResetTokenRepository != null && userLogin != null) {
            try {
                com.example.personalassistant.entity.PasswordResetToken prt = new com.example.personalassistant.entity.PasswordResetToken();
                prt.setToken(resetToken);
                prt.setExpiryDate(LocalDateTime.now().plusHours(1));
                prt.setUser(userLogin);
                passwordResetTokenRepository.save(prt);
            } catch (Exception ignored) {}
        }

        // Send generated password directly to the registered email
        emailService.sendNewPassword(destinationEmail, newPassword);

        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Password has been sent to your registered email address.",
                "resetToken", resetToken
        ));
    }

    public ResponseEntity<?> resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.status(400).body("token and password required");
        }

        // 1. Check in PasswordResetTokenRepository
        if (passwordResetTokenRepository != null) {
            java.util.Optional<com.example.personalassistant.entity.PasswordResetToken> prtOpt = passwordResetTokenRepository.findByToken(token);
            if (prtOpt.isPresent()) {
                com.example.personalassistant.entity.PasswordResetToken prt = prtOpt.get();
                if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
                    passwordResetTokenRepository.delete(prt);
                    return ResponseEntity.status(400).body("Token expired");
                }
                UserLogin login = prt.getUser();
                if (login != null) {
                    login.setPassword(passwordEncoder.encode(newPassword));
                    login.setAccountLocked(false);
                    login.setFailedLoginAttempts(0);
                    userLoginRepository.save(login);
                }
                passwordResetTokenRepository.delete(prt);
                return ResponseEntity.ok("Password reset successful! Your account is now unlocked.");
            }
        }

        // 2. Check in UserRepository (User.resetToken)
        User user = userRepository.findByResetToken(token);
        if (user != null) {
            if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(400).body("Token expired");
            }

            // Update password and unlock account in UserLogin
            UserLogin userLogin = userLoginRepository.findByEmail(user.getEmail()).orElse(null);
            if (userLogin != null) {
                userLogin.setPassword(passwordEncoder.encode(newPassword));
                userLogin.setAccountLocked(false); // UNLOCK
                userLogin.setFailedLoginAttempts(0); // RESET
                userLoginRepository.save(userLogin);
            }

            user.setResetToken(null);
            user.setTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok("Password reset successful! Your account is now unlocked.");
        }

        return ResponseEntity.status(400).body("Invalid token");
    }

    public ResponseEntity<Response> changePassword(String email, ChangePasswordDto dto) {

        UserLogin login = userLoginRepository.findById(email).orElse(null);
        if (login == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new Response(
                            null,
                            new ErrorDetails(HttpStatus.NOT_FOUND, "Account not found")
                    ));
        }

        // Check current password
        if (!passwordEncoder.matches(dto.getCurrentPassword(), login.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(new Response(
                            null,
                            new ErrorDetails(HttpStatus.BAD_REQUEST, "Incorrect current password")
                    ));
        }

        // Check new passwords match
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(new Response(
                            null,
                            new ErrorDetails(HttpStatus.BAD_REQUEST, "New passwords do not match")
                    ));
        }

        // Save new password
        login.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userLoginRepository.save(login);

        return ResponseEntity.ok(
                new Response(
                        "Password changed successfully",
                        null
                )
        );
    }


    public String extractEmailFromToken(String token) {
        token = token.replace("Bearer ", "");
        return jwtUtil.extractUsername(token);
    }

    public User getUserByEmail(String email) {
        Response response = new Response();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


}
