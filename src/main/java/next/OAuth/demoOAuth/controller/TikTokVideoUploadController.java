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
        
        if (principal == null) {
            System.err.println("Principal é null - usuário não autenticado");
            return "redirect:/login?error=not_authenticated";
        }

        String email = principal.getName();
        System.out.println("Email do usuário: " + email);
        
        Optional<User> userOpt = userService.findByEmail(email);
        if (!userOpt.isPresent()) {
            System.err.println("Usuário não encontrado no banco: " + email);
            return "redirect:/login?error=user_not_found";
        }

        User user = userOpt.get();
        
        Optional<UserProvider> tiktokProvider = user.getProviders().stream()
                .filter(p -> "TIKTOK".equals(p.getProvider()))
                .findFirst();

        if (!tiktokProvider.isPresent()) {
            System.err.println("Usuário não tem TikTok vinculado");
            return "redirect:/home?error=tiktok_not_linked";
        }

        UserProvider provider = tiktokProvider.get();
        
        System.out.println("✅ Usuário tem TikTok vinculado!");
        System.out.println("Access Token: " + (provider.getAccessToken() != null ? "Presente" : "Ausente"));
        System.out.println("Token expira em: " + provider.getTokenExpiresAt());

        model.addAttribute("userName", user.getName());
        model.addAttribute("hasValidToken", provider.getAccessToken() != null);

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
        System.out.println("===== Iniciando upload REAL de vídeo =====");
        
        if (principal == null) {
            return "redirect:/login?error=not_authenticated";
        }

        if (videoFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor, selecione um arquivo de vídeo");
            return "redirect:/tiktok/upload";
        }

        if (videoFile.getSize() > 100 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("error", "Arquivo muito grande! Tamanho máximo: 100MB");
            return "redirect:/tiktok/upload";
        }

        String contentType = videoFile.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            redirectAttributes.addFlashAttribute("error", "Por favor, envie um arquivo de vídeo válido");
            return "redirect:/tiktok/upload";
        }

        try {
            String email = principal.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            
            if (!userOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
                return "redirect:/tiktok/upload";
            }

            User user = userOpt.get();
            
            Optional<UserProvider> tiktokProvider = user.getProviders().stream()
                    .filter(p -> "TIKTOK".equals(p.getProvider()))
                    .findFirst();

            if (!tiktokProvider.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "TikTok não está vinculado");
                return "redirect:/tiktok/upload";
            }

            String accessToken = tiktokProvider.get().getAccessToken();
            
            if (accessToken == null) {
                redirectAttributes.addFlashAttribute("error", "Token de acesso ausente. Faça login novamente no TikTok.");
                return "redirect:/tiktok/upload";
            }

            System.out.println("Título: " + title);
            System.out.println("Descrição: " + description);
            System.out.println("Arquivo: " + videoFile.getOriginalFilename());
            System.out.println("Tamanho: " + videoFile.getSize() + " bytes");

            // ⭐ FAZER UPLOAD REAL PARA O TIKTOK
            String publishId = tikTokVideoUploadService.uploadVideoToTikTok(accessToken, videoFile);

            redirectAttributes.addFlashAttribute("success", 
                "✅ Vídeo enviado para sua caixa de entrada no TikTok! Publish ID: " + publishId);
            
            return "redirect:/tiktok/upload";

        } catch (Exception e) {
            System.err.println("Erro ao fazer upload: " + e.getMessage());
            e.printStackTrace();
            
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("401")) {
                errorMsg = "Token expirado. Faça login novamente no TikTok!";
            } else if (errorMsg != null && errorMsg.contains("403")) {
                errorMsg = "Sem permissão. Verifique se tem o escopo 'video.upload'.";
            }
            
            redirectAttributes.addFlashAttribute("error", "Erro: " + errorMsg);
            return "redirect:/tiktok/upload";
        }
    }
}
