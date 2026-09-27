package com.fixfirst;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    @Bean CommandLineRunner createAdmin(UserRepository users, PasswordEncoder encoder,
        @Value("${fixfirst.admin.email}") String email,
        @Value("${fixfirst.admin.password}") String password) {
        return args -> {
            if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("ADMIN_PASSWORD must be at least 12 characters and at most 72 UTF-8 bytes.");
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            var existing = users.findByEmail(normalized);
            if (existing.isEmpty()) {
                users.save(new AppUser("Maintenance Admin", normalized, encoder.encode(password), "ADMIN"));
            } else if (!existing.get().role.equals("ADMIN")) {
                throw new IllegalStateException("ADMIN_EMAIL already belongs to a student. Choose a different admin email.");
            }
            // Existing admin credentials are never silently overwritten.
        };
    }
}
