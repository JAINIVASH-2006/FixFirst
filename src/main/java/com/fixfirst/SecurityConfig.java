package com.fixfirst;

import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            AppUser u = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(u.email).password(u.passwordHash).roles(u.role).build();
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/api/csrf", "/api/register", "/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/technician/**").hasRole("TECHNICIAN")
                .anyRequest().authenticated())
            // CSRF remains enabled; the browser fetches a session token before each write.
            .formLogin(f -> f.loginProcessingUrl("/api/login")
                .successHandler((req,res,auth) -> res.setStatus(204))
                .failureHandler((req,res,ex) -> res.sendError(401, "Invalid email or password")))
            .logout(l -> l.logoutUrl("/api/logout")
                .logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> res.sendError(401))
                .accessDeniedHandler((req,res,ex) -> res.sendError(403)))
            .build();
    }
}
