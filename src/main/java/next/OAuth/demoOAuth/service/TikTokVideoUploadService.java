package next.OAuth.demoOAuth.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Service
public class TikTokVideoUploadService {

    private static final String TIKTOK_INBOX_INIT = "https://open.tiktokapis.com/v2/post/publish/inbox/video/init/";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadVideoToTikTokInbox(String accessToken, MultipartFile videoFile, String title, String description) throws Exception {
        System.out.println("🎥 INBOX UPLOAD TIKTOK - SEM AUDITÓRIA!");
        System.out.println("📹 Vídeo: " + videoFile.getOriginalFilename() + " (" + videoFile.getSize() + " bytes)");
        
        long videoSize = videoFile.getSize();

        // PASSO 1: INICIALIZAR (INBOX - SEM post_info)
        HttpHeaders initHeaders = new HttpHeaders();
        initHeaders.setBearerAuth(accessToken);
        initHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> sourceInfo = Map.of(
            "source", "FILE_UPLOAD",
            "video_size", (int)videoSize,
            "chunk_size", (int)videoSize,
            "total_chunk_count", 1
        );
        
        Map<String, Object> initBody = Map.of("source_info", sourceInfo);
        HttpEntity<Map<String, Object>> initRequest = new HttpEntity<>(initBody, initHeaders);
        
        System.out.println("🔄 PASSO 1: Inicializando INBOX...");
        ResponseEntity<Map> initResponse = restTemplate.postForEntity(TIKTOK_INBOX_INIT, initRequest, Map.class);
        
        if (initResponse.getStatusCodeValue() != 200) {
            throw new RuntimeException("Init falhou: " + initResponse.getBody());
        }
        
        Map<String, String> data = (Map<String, String>) initResponse.getBody().get("data");
        String publishId = data.get("publish_id");
        String uploadUrl = data.get("upload_url");
        
        System.out.println("✅ PASSO 1 OK! ID: " + publishId);

        // PASSO 2: UPLOAD VÍDEO
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.parseMediaType(videoFile.getContentType()));
        uploadHeaders.set("Content-Range", String.format("bytes 0-%d/%d", videoSize-1, videoSize));
        
        byte[] videoBytes = videoFile.getBytes();
        HttpEntity<byte[]> uploadRequest = new HttpEntity<>(videoBytes, uploadHeaders);
        
        System.out.println("📤 PASSO 2: Upload...");
        ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.PUT, uploadRequest, String.class);
        
        System.out.println("✅ PASSO 2 OK! Status: " + uploadResponse.getStatusCode());
        System.out.println("🎉 VÍDEO NA CAIXA DE ENTRADA TikTok!");
        
        return publishId;
    }
}
