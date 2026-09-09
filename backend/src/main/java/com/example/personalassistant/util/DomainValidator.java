package com.example.personalassistant.util;

import java.util.Set;
import java.util.regex.Pattern;

public class DomainValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}$"
    );

    // List of trusted genuine email domains
    private static final Set<String> GENUINE_DOMAINS = Set.of(
            "gmail.com",
            "googlemail.com",
            "yahoo.com",
            "ymail.com",
            "outlook.com",
            "hotmail.com",
            "live.com",
            "msn.com",
            "icloud.com",
            "me.com",
            "mac.com",
            "proton.me",
            "protonmail.com",
            "zoho.com",
            "aol.com",
            "rediffmail.com"
    );

    // Explicitly blocked disposable, temporary, or burner email domains
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com",
            "tempmail.com",
            "temp-mail.org",
            "10minutemail.com",
            "guerrillamail.com",
            "sharklasers.com",
            "yopmail.com",
            "trashmail.com",
            "getnada.com",
            "dispostable.com",
            "throwawaymail.com",
            "fakeinbox.com",
            "mohmal.com",
            "dropmail.me",
            "generator.email",
            "crazymailing.com",
            "inboxbear.com",
            "armyspy.com",
            "cuvox.de",
            "dayrep.com",
            "fleckens.hu",
            "gustr.com",
            "jourrapide.com",
            "rhyta.com",
            "superrito.com",
            "teleworm.us"
    );

    public static boolean isGenuineDomain(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String clean = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(clean).matches()) {
            return false;
        }

        int atIndex = clean.lastIndexOf('@');
        if (atIndex == -1 || atIndex == clean.length() - 1) {
            return false;
        }

        String domain = clean.substring(atIndex + 1);

        // Reject known disposable domains immediately
        if (DISPOSABLE_DOMAINS.contains(domain)) {
            return false;
        }

        // Allow trusted genuine email providers
        if (GENUINE_DOMAINS.contains(domain)) {
            return true;
        }

        // For business or university domains (must have valid structure and TLD)
        // Ensure no subdomain of disposable domains
        for (String disp : DISPOSABLE_DOMAINS) {
            if (domain.endsWith("." + disp)) {
                return false;
            }
        }

        // Allow genuine institutional or enterprise domains if valid
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }
}
