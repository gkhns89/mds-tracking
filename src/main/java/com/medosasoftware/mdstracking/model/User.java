package com.medosasoftware.mdstracking.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // Role enum kullanımı

    // Getter & Setter metodu Lombok sayesinde otomatik olarak ekleniyor
    @Getter
    @Setter
    @ManyToMany
    @JoinTable(
            name = "user_companies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    private List<Company> companies = new ArrayList<>();  // ✅ Liste başlatıldı

    // Constructor ve diğer gerekli metotlar
    public User() {
    }

}
