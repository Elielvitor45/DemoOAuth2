package next.OAuth.demoOAuth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
public class TikTokOAuthService {

    @Value("${tiktok.client-key}")
    private String clientKey;

    @Value("${tiktok.client-secret}")
    private String clientSecret;

    @Value("${tiktok.redirect-uri}")
    private String redirectUri;

    @Value("${tiktok.authorization-uri}")
    private String authorizationUri;

    @Value("${tiktok.token-uri}")
    private String tokenUri;

    @Value("${tiktok.user-info-uri}")
    private String userInfoUri;

    @Value("${tiktok.scope}")
    private String scope;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateState() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] state = new byte[16];
        secureRandom.nextBytes(state);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(state);
    }

    public String buildAuthorizationUrl(String state) {
        return authorizationUri +
                "?client_key=" + clientKey +
                "&response_type=code" +
                "&scope=" + scope +
                "&redirect_uri=" + redirectUri +
                "&state=" + state;
    }

    public Map<String, Object> exchangeCodeForToken(String code) {
        try {
            System.out.println("===== Iniciando troca de token =====");
            System.out.println("Token URI: " + tokenUri);
            System.out.println("Client Key: " + clientKey);
            System.out.println("Redirect URI: " + redirectUri);
            System.out.println("Code: " + code.substring(0, Math.min(20, code.length())) + "...");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Cache-Control", "no-cache");

            // URL encode manual dos parâmetros
            String encodedBody = "client_key=" + java.net.URLEncoder.encode(clientKey, StandardCharsets.UTF_8) +
                    "&client_secret=" + java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&code=" + java.net.URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&grant_type=" + java.net.URLEncoder.encode("authorization_code", StandardCharsets.UTF_8) +
                    "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

            System.out.println("===== Body encodificado =====");
            System.out.println(encodedBody.substring(0, Math.min(150, encodedBody.length())) + "...");

            HttpEntity<String> request = new HttpEntity<>(encodedBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    request,
                    String.class);

            System.out.println("===== TikTok Token Response Status =====");
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("===== TikTok Token Response Body =====");
            System.out.println(response.getBody());

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

            if (responseMap.containsKey("error")) {
                String error = (String) responseMap.get("error");
                String errorDescription = (String) responseMap.get("error_description");
                throw new RuntimeException("Erro do TikTok: " + error + " - " + errorDescription);
            }

            return responseMap;
        } catch (Exception e) {
            System.err.println("===== ERRO COMPLETO ao trocar code por token =====");
            System.err.println("Mensagem: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao obter token do TikTok: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{\"fields\":[\"open_id\",\"union_id\",\"avatar_url\",\"display_name\"]}";

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.POST,
                    request,
                    String.class);

            System.out.println("===== TikTok UserInfo Response =====");
            System.out.println(response.getBody());

            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            System.err.println("Erro ao buscar userInfo: " + e.getMessage());
            throw new RuntimeException("Erro ao buscar dados do usuário TikTok", e);
        }
    }

    public LocalDateTime calculateTokenExpiration(int expiresIn) {
        return LocalDateTime.now().plusSeconds(expiresIn);
    }
}
