package com.example.api.infrastructure.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Troca o authorization_code pelo access_token do Google
 * e busca os dados do usuário via OIDC userinfo endpoint.
 *
 * O redirect_uri usado aqui é: http://localhost:8080/login/oauth2/code/google
 * Este valor DEVE estar cadastrado no Google Console como URI autorizada.
 */
@Component
@Slf4j
public class GoogleOAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.oauth2.google.client-id}")      private String clientId;
    @Value("${app.oauth2.google.client-secret}")  private String clientSecret;
    @Value("${app.oauth2.google.token-uri}")      private String tokenUri;
    @Value("${app.oauth2.google.user-info-uri}")  private String userInfoUri;
    @Value("${app.oauth2.google.redirect-uri}")   private String redirectUri;

    @SuppressWarnings("unchecked")
    public OAuthTokenData exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "authorization_code");
        body.add("code",          code);
        body.add("redirect_uri",  redirectUri);
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);

        ResponseEntity<Map> response = restTemplate.exchange(
            tokenUri, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> resp = response.getBody();
        if (resp == null || !resp.containsKey("access_token")) {
            throw new RuntimeException("Google: falha ao trocar code por token");
        }

        String accessToken = (String) resp.get("access_token");
        Integer expiresIn  = (Integer) resp.get("expires_in");
        LocalDateTime expiresAt = expiresIn != null
            ? LocalDateTime.now().plusSeconds(expiresIn) : null;

        log.debug("Google token obtido com sucesso");
        return new OAuthTokenData(accessToken, expiresAt);
    }

    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> response = restTemplate.exchange(
            userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        Map<String, Object> info = response.getBody();
        if (info == null) throw new RuntimeException("Google: userinfo vazio");

        return OAuthUserInfo.builder()
            .providerId((String) info.get("sub"))
            .email((String) info.get("email"))
            .name((String) info.get("name"))
            .photoUrl((String) info.get("picture"))
            .build();
    }
}
