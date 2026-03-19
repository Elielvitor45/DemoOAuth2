package com.example.api.infrastructure.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Troca o code do Facebook pelo access_token e busca dados do usuário.
 * Particularidade: picture vem aninhado em {picture: {data: {url, ...}}}.
 *
 * Redirect URI: http://localhost:8080/login/oauth2/code/facebook
 */
@Component
@Slf4j
public class FacebookOAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.oauth2.facebook.client-id}")      private String clientId;
    @Value("${app.oauth2.facebook.client-secret}")  private String clientSecret;
    @Value("${app.oauth2.facebook.token-uri}")      private String tokenUri;
    @Value("${app.oauth2.facebook.user-info-uri}")  private String userInfoUri;
    @Value("${app.oauth2.facebook.redirect-uri}")   private String redirectUri;

    @SuppressWarnings("unchecked")
    public OAuthTokenData exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("code",          code);
        body.add("redirect_uri",  redirectUri);

        ResponseEntity<Map> response = restTemplate.exchange(
            tokenUri, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> resp = response.getBody();
        if (resp == null || !resp.containsKey("access_token")) {
            throw new RuntimeException("Facebook: falha ao trocar code por token");
        }

        String accessToken = (String) resp.get("access_token");
        Integer expiresIn  = (Integer) resp.get("expires_in");
        LocalDateTime expiresAt = expiresIn != null
            ? LocalDateTime.now().plusSeconds(expiresIn) : null;

        log.debug("Facebook token obtido com sucesso");
        return new OAuthTokenData(accessToken, expiresAt);
    }

    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        String url = UriComponentsBuilder.fromUriString(userInfoUri)
            .queryParam("access_token", accessToken)
            .toUriString();

        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, HttpEntity.EMPTY, Map.class);

        Map<String, Object> info = response.getBody();
        if (info == null) throw new RuntimeException("Facebook: userinfo vazio");

        // Extrai foto do objeto aninhado picture.data.url
        String photoUrl = null;
        try {
            Map<String, Object> picture     = (Map<String, Object>) info.get("picture");
            Map<String, Object> pictureData = (Map<String, Object>) picture.get("data");
            photoUrl = (String) pictureData.get("url");
        } catch (Exception ignored) { /* foto opcional */ }

        return OAuthUserInfo.builder()
            .providerId((String) info.get("id"))
            .email((String) info.get("email"))
            .name((String) info.get("name"))
            .photoUrl(photoUrl)
            .build();
    }
}
