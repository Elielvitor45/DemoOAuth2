package next.OAuth.demoOAuth.controller;

import jakarta.servlet.http.HttpSession;
import next.OAuth.demoOAuth.model.User;
import next.OAuth.demoOAuth.model.UserProvider;
import next.OAuth.demoOAuth.repository.UserProviderRepository;
import next.OAuth.demoOAuth.service.TikTokVideoUploadService;
import next.OAuth.demoOAuth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/tiktok")
public class TikTokVideoUploadController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserProviderRepository userProviderRepository;

    @Autowired
    private TikTokVideoUploadService tikTokVideoUploadService;

    @GetMapping("/upload")
    public String showUploadPage(Principal principal, Model model) {
        System.out.println("===== Acessando /tiktok/upload =====");
        
        if (principal == null || !principal.getName().startsWith("tiktok_")) {
            model.addAttribute("error", "Login com TikTok necessário!");
            model.addAttribute("uploadReady", false);
            return "tiktok-upload";
        }

        String openId = principal.getName().substring(7);
        System.out.println("TikTok OpenID: " + openId);
        
        Optional<UserProvider> providerOpt = userProviderRepository
            .findByProviderAndProviderId("TIKTOK", openId);
        
        if (!providerOpt.isPresent()) {
            model.addAttribute("error", "Conta TikTok não encontrada!");
            model.addAttribute("uploadReady", false);
            return "tiktok-upload";
        }

        UserProvider provider = providerOpt.get();
        User user = provider.getUser();
        
        System.out.println("✅ Usuário TikTok pronto para upload!");
        System.out.println("Nome: " + user.getName());
        System.out.println("Access Token: " + (provider.getAccessToken() != null ? "OK" : "NULL"));

        model.addAttribute("userName", user.getName());
        model.addAttribute("hasValidToken", provider.getAccessToken() != null);
        model.addAttribute("uploadReady", true);

        return "tiktok-upload";
    }

    @PostMapping("/upload")
    public String uploadVideo(
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("===== Iniciando UPLOAD REAL para TikTok =====");
        
        if (principal == null || !principal.getName().startsWith("tiktok_")) {
            redirectAttributes.addFlashAttribute("error", "Login TikTok necessário!");
            return "redirect:/tiktok/upload";
        }

        if (videoFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Selecione um vídeo!");
            return "redirect:/tiktok/upload";
        }

        try {
            String openId = principal.getName().substring(7);
            Optional<UserProvider> providerOpt = userProviderRepository
                .findByProviderAndProviderId("TIKTOK", openId);

            if (!providerOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "TikTok não vinculado!");
                return "redirect:/tiktok/upload";
            }

            UserProvider provider = providerOpt.get();
            String accessToken = provider.getAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Token inválido. Faça login novamente!");
                return "redirect:/tiktok/upload";
            }

            // ✅ USA INBOX ENDPOINT (sem 403)
            String publishId = tikTokVideoUploadService.uploadVideoToTikTokInbox(
                accessToken, videoFile, title, description);

            redirectAttributes.addFlashAttribute("success", 
                "✅ Vídeo enviado para CAIXA DE ENTRADA! ID: " + publishId + 
                " 📱 Abra TikTok → Notificações → Publicar");

        } catch (Exception e) {
            System.err.println("💥 Erro upload: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erro: " + e.getMessage());
        }
        
        return "redirect:/tiktok/upload";
    }
}
