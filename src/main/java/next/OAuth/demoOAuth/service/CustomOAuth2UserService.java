package next.OAuth.demoOAuth.service;

import next.OAuth.demoOAuth.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Autowired
    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Captura access token e expiração
        String accessToken = userRequest.getAccessToken().getTokenValue();
        Instant expiresAt = userRequest.getAccessToken().getExpiresAt();
        LocalDateTime tokenExpiresAt = expiresAt != null ? LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
                : null;

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("id") != null ? oauth2User.getAttribute("id").toString()
                : oauth2User.getAttribute("sub");
        String photoUrl = null;

        System.out.println("========================================");
        System.out.println("===== LOGIN VIA OAUTH =====");
        System.out.println("Provider: " + provider);
        System.out.println("Provider ID: " + providerId);
        System.out.println("Email inicial: " + email);
        System.out.println("Nome: " + name);
        System.out.println("Access Token: " + accessToken.substring(0, Math.min(30, accessToken.length())) + "...");
        System.out.println("Token expira em: " + tokenExpiresAt);

        // Captura foto de perfil do Facebook
        if ("facebook".equalsIgnoreCase(provider)) {
            Map<String, Object> picture = oauth2User.getAttribute("picture");
            if (picture != null) {
                Map<String, Object> data = (Map<String, Object>) picture.get("data");
                if (data != null) {
                    photoUrl = (String) data.get("url");
                    System.out.println("Foto do Facebook: " + photoUrl);
                }
            }
        }

        // Captura foto de perfil do GitHub
        if ("github".equalsIgnoreCase(provider)) {
            photoUrl = oauth2User.getAttribute("avatar_url");
            System.out.println("Foto do GitHub: " + photoUrl);

            if (email == null || email.isEmpty()) {
                System.out.println("===== Email null no GitHub, buscando via API =====");
                email = getGitHubPrimaryEmail(accessToken);
                System.out.println("Email obtido via API: " + email);
            }
        }
        // Depois do tratamento do GitHub, adicione:

        // Captura dados do TikTok
        if ("tiktok".equalsIgnoreCase(provider)) {
            // TikTok retorna estrutura diferente: data.user.display_name
            Map<String, Object> data = oauth2User.getAttribute("data");
            if (data != null) {
                Map<String, Object> userInfo = (Map<String, Object>) data.get("user");
                if (userInfo != null) {
                    name = (String) userInfo.get("display_name");
                    photoUrl = (String) userInfo.get("avatar_url");
                    providerId = (String) userInfo.get("open_id");

                    // TikTok não fornece email por padrão
                    email = providerId + "@tiktok.placeholder";

                    System.out.println("Nome do TikTok: " + name);
                    System.out.println("Foto do TikTok: " + photoUrl);
                }
            }
        }

        System.out.println("========================================");

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email não fornecido pelo provedor " + provider);
        }

        // Salva com access token, expiração e foto
        userService.findOrCreateOAuthUser(email, name, provider.toUpperCase(), providerId,
                accessToken, tokenExpiresAt, null, photoUrl);

        return oauth2User;
    }

    private String getGitHubPrimaryEmail(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            if (response.getBody() != null) {
                for (Map<String, Object> emailData : response.getBody()) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    Boolean verified = (Boolean) emailData.get("verified");

                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }

                // Se não encontrou primary, pega o primeiro verificado
                for (Map<String, Object> emailData : response.getBody()) {
                    Boolean verified = (Boolean) emailData.get("verified");
                    if (Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar email do GitHub: " + e.getMessage());
        }

        return null;
    }
}
