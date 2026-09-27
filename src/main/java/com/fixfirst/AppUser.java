package com.fixfirst;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 120)
    public String email;
    @Column(nullable = false, length = 60)
    public String name;
    @Column(nullable = false)
    public String passwordHash;
    @Column(nullable = false)
    public String role;
    protected AppUser() {}
    public AppUser(String name, String email, String hash, String role) {
        this.name = name; this.email = email; this.passwordHash = hash; this.role = role;
    }
}
