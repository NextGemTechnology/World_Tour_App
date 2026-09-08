package com.example.personalassistant.config;

import com.example.personalassistant.entity.Admin;
import com.example.personalassistant.entity.AdminLogin;
import com.example.personalassistant.entity.User;
import com.example.personalassistant.entity.UserLogin;
import com.example.personalassistant.enums.AccountEnum;
import com.example.personalassistant.repository.AdminLoginRepository;
import com.example.personalassistant.repository.AdminRepository;
import com.example.personalassistant.repository.UserLoginRepository;
import com.example.personalassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminLoginRepository adminLoginRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${admin.default.email:admin@nextgem.com}")
    private String defaultAdminEmail;

    @org.springframework.beans.factory.annotation.Value("${admin.default.password:AdminSecretPass123!}")
    private String defaultAdminPass;

    @org.springframework.beans.factory.annotation.Value("${seed.user.email:guest@worldtours.com}")
    private String userEmail;

    @org.springframework.beans.factory.annotation.Value("${seed.user.password:Password123!}")
    private String userPass;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Super Admin
        if (adminLoginRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setName("Super Admin");
            admin.setEmail(defaultAdminEmail);
            admin.setNumber(9999999999L);
            adminRepository.save(admin);

            AdminLogin adminLogin = new AdminLogin();
            adminLogin.setEmail(defaultAdminEmail);
            adminLogin.setPassword(passwordEncoder.encode(defaultAdminPass));
            adminLoginRepository.save(adminLogin);

            System.out.println("✅ DEFAULT SUPER ADMIN SEEDED: " + defaultAdminEmail);
        }

        // 2. Seed Default Verified User
        if (userLoginRepository.findByEmail(userEmail).isEmpty()) {
            if (userRepository.findByEmail(userEmail).isEmpty()) {
                User user = new User();
                user.setName("Hotel LuxNes");
                user.setEmail(userEmail);
                user.setMobile("9876500001");
                user.setCity("New Delhi");
                user.setAccountEnum(AccountEnum.ACTIVE);
                userRepository.save(user);
            }

            UserLogin userLogin = new UserLogin();
            userLogin.setEmail(userEmail);
            userLogin.setPassword(passwordEncoder.encode(userPass));
            userLogin.setEmailVerified(true);
            userLogin.setTwoFactorEnabled(false);
            userLogin.setAccountLocked(false);
            userLogin.setFailedLoginAttempts(0);
            userLoginRepository.save(userLogin);

            System.out.println("✅ DEFAULT USER SEEDED: " + userEmail + " (password: " + userPass + ")");
        }
    }
}
