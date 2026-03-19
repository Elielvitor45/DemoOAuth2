package com.example.api.domain.model;

import com.example.api.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 255)
    private String email;

    // NULL para usuários que só usam OAuth (nunca criaram senha)
    @Column(nullable = true, length = 255)
    private String password;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "photo_url", nullable = true, length = 500)
    private String photoUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserProvider> providers = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addProvider(UserProvider provider) {
        provider.setUser(this);
        this.providers.add(provider);
    }

    public boolean hasProvider(AuthProvider authProvider) {
        return this.providers.stream().anyMatch(p -> p.getProvider() == authProvider);
    }

    public boolean hasPassword() {
        return this.password != null && !this.password.isBlank();
    }
}
