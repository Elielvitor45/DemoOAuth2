package com.example.api.domain.model;

import com.example.api.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_providers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_provider_provider_id", columnNames = {"provider", "provider_id"})
    },
    indexes = {
        @Index(name = "idx_up_user_id", columnList = "user_id"),
        @Index(name = "idx_up_provider", columnList = "provider, provider_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * columnDefinition = "VARCHAR(20)" é obrigatório aqui.
     * O Hibernate 6 por padrão tenta mapear @Enumerated(STRING) como ENUM do MySQL.
     * Mas a migration criou VARCHAR(20). Sem este atributo, a validação do schema
     * falha com: "found [varchar], but expecting [enum(...)]"
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = true, length = 255)
    private String providerId;

    @Column(name = "access_token", nullable = true, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "token_expires_at", nullable = true)
    private LocalDateTime tokenExpiresAt;

    @CreatedDate
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @LastModifiedDate
    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;
}
