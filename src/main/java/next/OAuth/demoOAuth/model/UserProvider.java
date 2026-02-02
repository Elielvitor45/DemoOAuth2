package next.OAuth.demoOAuth.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_providers", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
public class UserProvider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String provider; // GOOGLE, FACEBOOK, GITHUB, LOCAL
    
    @Column(name = "provider_id")
    private String providerId; // ID do usuário no provedor (null para LOCAL)
    
    // NOVOS CAMPOS - OAuth Token Data
    @Column(name = "access_token", length = 2000)
    private String accessToken;
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;
    
    // Datas de controle
    @Column(name = "linked_at")
    private LocalDateTime linkedAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @PrePersist
    protected void onCreate() {
        linkedAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUsedAt = LocalDateTime.now();
    }
    
    // Construtores
    public UserProvider() {}
    
    public UserProvider(User user, String provider, String providerId) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }
    
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }
    
    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    // Método útil para verificar se o token ainda é válido
    public boolean isTokenValid() {
        if (tokenExpiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(tokenExpiresAt);
    }
}
