package next.OAuth.demoOAuth.service;

import next.OAuth.demoOAuth.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserService userService;

    @Autowired
    public CustomOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Captura access token e expiração
        String accessToken = userRequest.getAccessToken().getTokenValue();
        Instant expiresAt = userRequest.getAccessToken().getExpiresAt();
        LocalDateTime tokenExpiresAt = expiresAt != null ? 
            LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()) : null;

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String providerId = oidcUser.getSubject();
        String photoUrl = oidcUser.getPicture(); // CAPTURA A FOTO DO GOOGLE

        System.out.println("========================================");
        System.out.println("===== LOGIN VIA OIDC (Google) =====");
        System.out.println("Provider: " + provider);
        System.out.println("Provider ID: " + providerId);
        System.out.println("Email: " + email);
        System.out.println("Nome: " + name);
        System.out.println("Foto: " + photoUrl);
        System.out.println("Access Token: " + accessToken.substring(0, Math.min(30, accessToken.length())) + "...");
        System.out.println("Token expira em: " + tokenExpiresAt);
        System.out.println("========================================");

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email não fornecido pelo provedor " + provider);
        }

        // Salva com access token, expiração E FOTO
        userService.findOrCreateOAuthUser(email, name, provider.toUpperCase(), providerId, 
                                         accessToken, tokenExpiresAt, null, photoUrl);

        return oidcUser;
    }
}
