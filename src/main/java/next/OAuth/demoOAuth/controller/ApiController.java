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
import next.OAuth.demoOAuth.repository.UserRepository;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import next.OAuth.demoOAuth.service.UserService;
import java.util.List;

@Controller
public class ApiController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    
    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        String name = "Usuário";
        String email = "";
        String photoUrl = null;
        List<String> providers = new ArrayList<>();
        String currentProvider = "LOCAL";

        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) principal;
            currentProvider = oauthToken.getAuthorizedClientRegistrationId().toUpperCase();

            Object principalObj = oauthToken.getPrincipal();
            String providerId = null;

            if (principalObj instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principalObj;
                providerId = oidcUser.getSubject();
            } else if (principalObj instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principalObj;
                providerId = oauth2User.getAttribute("id") != null ? oauth2User.getAttribute("id").toString()
                        : oauth2User.getAttribute("sub");
            }

            // Buscar usuário do banco
            Optional<User> userOpt = userService.findByProviderAndProviderId(currentProvider, providerId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                email = user.getEmail();
                name = user.getName();
                photoUrl = user.getPhotoUrl();

                // Buscar todos os provedores vinculados
                providers = user.getProviders().stream()
                        .map(UserProvider::getProvider)
                        .collect(Collectors.toList());
            }
        } else {
            email = principal.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                name = user.getName();
                photoUrl = user.getPhotoUrl();
                providers = user.getProviders().stream()
                        .map(UserProvider::getProvider)
                        .collect(Collectors.toList());
            }
        }

        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("photoUrl", photoUrl);
        model.addAttribute("currentProvider", currentProvider);
        model.addAttribute("providers", providers);

        return "home";
    }
}
