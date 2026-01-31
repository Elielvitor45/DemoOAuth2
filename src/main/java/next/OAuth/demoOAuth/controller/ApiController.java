package next.OAuth.demoOAuth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import next.OAuth.demoOAuth.model.User;
import next.OAuth.demoOAuth.model.UserProvider;
import next.OAuth.demoOAuth.repository.UserProviderRepository;
import next.OAuth.demoOAuth.repository.UserRepository;
import next.OAuth.demoOAuth.service.UserService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ApiController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProviderRepository userProviderRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        String name = "Usuário";
        String displayEmail = "";
        String photoUrl = null;
        List<String> providers = new ArrayList<>();
        String currentProvider = "LOCAL";

        if (principal.getName() != null && principal.getName().startsWith("tiktok_")) {
            // ⭐ TIKTOK LOGIN
            String openId = principal.getName().substring(7);
            System.out.println("🔍 TikTok OpenID: " + openId);
            
            Optional<UserProvider> providerOpt = userProviderRepository
                .findByProviderAndProviderId("TIKTOK", openId);
                
            if (providerOpt.isPresent()) {
                UserProvider provider = providerOpt.get();
                User user = provider.getUser();
                
                currentProvider = "TIKTOK";
                providers.add("TIKTOK");
                
                name = user.getName() != null ? user.getName() : "TikTok User";
                displayEmail = "@TikTok";
                photoUrl = user.getPhotoUrl();
            }
            
        } else if (principal instanceof OAuth2AuthenticationToken) {
            // ⭐ GOOGLE/GITHUB/FACEBOOK
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) principal;
            currentProvider = oauthToken.getAuthorizedClientRegistrationId().toUpperCase();

            Object principalObj = oauthToken.getPrincipal();
            String providerId = null;

            if (principalObj instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principalObj;
                providerId = oidcUser.getSubject();
            } else if (principalObj instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principalObj;
                providerId = oauth2User.getAttribute("id") != null ? 
                    oauth2User.getAttribute("id").toString() : oauth2User.getAttribute("sub");
            }

            Optional<User> userOpt = userService.findByProviderAndProviderId(currentProvider, providerId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                displayEmail = user.getEmail() != null ? user.getEmail() : "";
                name = user.getName();
                photoUrl = user.getPhotoUrl();

                providers = user.getProviders().stream()
                    .map(UserProvider::getProvider)
                    .collect(Collectors.toList());
            }
            
        } else {
            // ⭐ LOGIN LOCAL
            String email = principal.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                name = user.getName();
                photoUrl = user.getPhotoUrl();
                displayEmail = email;
                
                providers = user.getProviders().stream()
                    .map(UserProvider::getProvider)
                    .collect(Collectors.toList());
                
                if (providers.contains("LOCAL")) {
                    currentProvider = "LOCAL";
                }
            }
        }

        // DEBUG
        System.out.println("===== DEBUG HOME =====");
        System.out.println("Name: " + name);
        System.out.println("Email: " + displayEmail);
        System.out.println("Photo: " + photoUrl);
        System.out.println("Provider: " + currentProvider);
        System.out.println("Providers: " + providers);
        System.out.println("=====================");

        model.addAttribute("name", name);
        model.addAttribute("email", displayEmail);
        model.addAttribute("photoUrl", photoUrl);
        model.addAttribute("currentProvider", currentProvider);
        model.addAttribute("providers", providers);

        return "home";
    }
}
