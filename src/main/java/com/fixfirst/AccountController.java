package com.fixfirst;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public AccountController(UserRepository users, PasswordEncoder encoder) { this.users=users;this.encoder=encoder; }
    public record Registration(@NotBlank @Size(max=60) String name,
        @NotBlank @Email @Size(max=120) String email, @NotBlank @Size(min=10,max=72) String password) {}
    public record TechnicianInput(@NotBlank @Size(max=60) String name,
        @NotBlank @Email @Size(max=120) String email, @NotBlank @Size(min=10,max=72) String password) {}
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> register(@Valid @RequestBody Registration input) {
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes.");
        String email=input.email().trim().toLowerCase(Locale.ROOT);
        if(users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered.");
        users.save(new AppUser(input.name().trim(),email,encoder.encode(input.password()),"STUDENT"));
        return Map.of("message","Account created. You can now sign in.");
    }
    @GetMapping("/me") public Map<String,Object> me(Principal principal) {
        AppUser u=users.findByEmail(principal.getName()).orElseThrow();
        return Map.of("id",u.id,"name",u.name,"email",u.email,"role",u.role);
    }
    @GetMapping("/admin/technicians") public List<Map<String,Object>> technicians() {
        return users.findByRole("TECHNICIAN").stream()
            .map(u -> Map.<String,Object>of("id",u.id,"name",u.name,"email",u.email,"role",u.role)).toList();
    }
    @PostMapping("/admin/technicians") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createTechnician(@Valid @RequestBody TechnicianInput input) {
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes.");
        String email=input.email().trim().toLowerCase(Locale.ROOT);
        if(users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered.");
        AppUser saved=users.save(new AppUser(input.name().trim(),email,encoder.encode(input.password()),"TECHNICIAN"));
        return Map.of("id",saved.id,"name",saved.name,"email",saved.email,"role",saved.role);
    }
}
