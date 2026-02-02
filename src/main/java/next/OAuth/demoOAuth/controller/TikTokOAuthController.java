package next.OAuth.demoOAuth.controller;

import javax.servlet.http.HttpSession;
import next.OAuth.demoOAuth.model.User;
import next.OAuth.demoOAuth.model.UserProvider;
import next.OAuth.demoOAuth.repository.UserProviderRepository;
import next.OAuth.demoOAuth.repository.UserRepository;
import next.OAuth.demoOAuth.service.TikTokOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class TikTokOAuthController {

    @Autowired
    private TikTokOAuthService tikTokOAuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProviderRepository userProviderRepository;

    @GetMapping("/oauth/tiktok")
    public String initiateTikTokLogin(HttpSession session) {
        String state = tikTokOAuthService.generateState();
        
        session.setAttribute("tiktok_state", state);

        System.out.println("===== Iniciando login TikTok =====");
        System.out.println("State: " + state);

        String authUrl = tikTokOAuthService.buildAuthorizationUrl(state);
        
        System.out.println("URL de autorização: " + authUrl);
        
        return "redirect:" + authUrl;
    }

    @GetMapping("/auth/tiktok/callback/")
    public String handleTikTokCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "scopes", required = false) String scopes,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("===== TikTok Callback Recebido =====");
        System.out.println("Code: " + (code != null ? code.substring(0, Math.min(20, code.length())) + "..." : "null"));
        System.out.println("State: " + state);
        System.out.println("Scopes: " + scopes);
        System.out.println("Error: " + error);
        System.out.println("Error Description: " + errorDescription);

        if (error != null) {
            System.err.println("Erro do TikTok: " + error + " - " + errorDescription);
            return "redirect:/login?error=" + error;
        }

        if (code == null || state == null) {
            System.err.println("Code ou state ausente no callback");
            return "redirect:/login?error=missing_params";
        }

        String savedState = (String) session.getAttribute("tiktok_state");
        if (savedState == null || !savedState.equals(state)) {
            System.err.println("State inválido. Esperado: " + savedState + ", Recebido: " + state);
            return "redirect:/login?error=invalid_state";
        }

        try {
            // 1. Troca o code pelo token
            Map<String, Object> tokenResponse = tikTokOAuthService.exchangeCodeForToken(code);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");
            String openId = (String) tokenResponse.get("open_id");

            if (accessToken == null || openId == null) {
                System.err.println("Access token ou Open ID ausente na resposta");
                return "redirect:/login?error=tiktok_invalid_response";
            }

            System.out.println("===== Tokens obtidos com sucesso! =====");
            System.out.println("Open ID: " + openId);
            System.out.println("Scopes: " + tokenResponse.get("scope"));

            // 2. ⭐ BUSCAR DADOS DINÂMICOS DO USUÁRIO
            Map<String, Object> userInfo = tikTokOAuthService.getUserInfo(accessToken, openId);
            String displayName = "TikTok User";
            String avatarUrl = null;
            String username = null;

            if (userInfo != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) userInfo.get("data");
                    if (data != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> userData = (Map<String, Object>) data.get("user");
                        if (userData != null) {
                            displayName = (String) userData.get("display_name");
                            avatarUrl = (String) userData.get("avatar_url");
                            username = (String) userData.get("username");
                            
                            System.out.println("🎉 DADOS DINÂMICOS:");
                            System.out.println("Display Name: " + displayName);
                            System.out.println("Username: " + username);
                            System.out.println("Avatar URL: " + avatarUrl);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao parsear userInfo: " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ getUserInfo retornou null - usando dados básicos");
                displayName = "TikTok User - " + openId.substring(0, 8);
            }

            // 3. Buscar ou criar usuário no banco
            Optional<UserProvider> existingProvider = userProviderRepository
                    .findByProviderAndProviderId("TIKTOK", openId);

            User user;
            UserProvider userProvider;

            if (existingProvider.isPresent()) {
                // Usuário já existe - ATUALIZAR dados
                System.out.println("👤 Usuário TikTok já existe no banco!");
                userProvider = existingProvider.get();
                user = userProvider.getUser();

                // Atualizar dados dinâmicos
                user.setName(displayName);
                if (avatarUrl != null) {
                    user.setPhotoUrl(avatarUrl);
                }
                userRepository.save(user);

            } else {
                // Criar novo usuário
                System.out.println("🆕 Criando novo usuário TikTok...");
                
                user = new User();
                user.setName(displayName);
                user.setEmail(null); // NULL conforme pedido
                user.setPhotoUrl(avatarUrl);
                user = userRepository.save(user);

                // Criar UserProvider
                userProvider = new UserProvider();
                userProvider.setUser(user);
                userProvider.setProvider("TIKTOK");
                userProvider.setProviderId(openId);
                userProvider.setAccessToken(accessToken);
                userProvider.setRefreshToken(refreshToken);
                userProvider.setTokenExpiresAt(tikTokOAuthService.calculateTokenExpiration(expiresIn));
                userProviderRepository.save(userProvider);

                System.out.println("✅ Usuário criado com ID: " + user.getId());
            }

            // 4. SEMPRE atualizar tokens
            userProvider.setAccessToken(accessToken);
            userProvider.setRefreshToken(refreshToken);
            userProvider.setTokenExpiresAt(tikTokOAuthService.calculateTokenExpiration(expiresIn));
            userProvider.setLastUsedAt(LocalDateTime.now());
            userProviderRepository.save(userProvider);

            // 5. AUTENTICAR NO SPRING SECURITY
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "tiktok_" + openId, // Identificador único e dinâmico
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            System.out.println("🎉 ===== USUÁRIO AUTENTICADO COM SUCESSO! =====");
            System.out.println("Nome: " + user.getName());
            System.out.println("Username: " + username);
            System.out.println("OpenID: " + openId);
            System.out.println("Redirecionando para /home");

            return "redirect:/home";

        } catch (Exception e) {
            System.err.println("💥 Erro no callback TikTok: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error=tiktok_callback_failed";
        }
    }
}
