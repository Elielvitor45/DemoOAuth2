package next.OAuth.demoOAuth.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class TikTokVideoUploadService {

    private static final String TIKTOK_API_BASE = "https://open.tiktokapis.com";
    private static final String INIT_UPLOAD_ENDPOINT = "/v2/post/publish/inbox/video/init/";

    public Map<String, Object> initializeUpload(String accessToken, long videoSize) {
        RestTemplate restTemplate = new RestTemplate();
        
        String url = TIKTOK_API_BASE + INIT_UPLOAD_ENDPOINT;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> sourceInfo = new HashMap<>();
        sourceInfo.put("source", "FILE_UPLOAD");
        sourceInfo.put("video_size", videoSize);
        sourceInfo.put("chunk_size", videoSize);
        sourceInfo.put("total_chunk_count", 1);
        requestBody.put("source_info", sourceInfo);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        System.out.println("===== Inicializando upload no TikTok =====");
        System.out.println("URL: " + url);
        System.out.println("Video Size: " + videoSize + " bytes");
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            System.out.println("===== Resposta TikTok Init Upload =====");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                
                if (data != null) {
                    return data;
                }
            }
            
            throw new RuntimeException("Falha ao inicializar upload: " + response.getBody());
            
        } catch (Exception e) {
            System.err.println("Erro ao inicializar upload: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao inicializar upload: " + e.getMessage(), e);
        }
    }

    public void uploadVideoFile(String uploadUrl, MultipartFile videoFile) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(videoFile.getSize());
        
        byte[] videoBytes = videoFile.getBytes();
        HttpEntity<byte[]> request = new HttpEntity<>(videoBytes, headers);
        
        System.out.println("===== Fazendo upload do vídeo =====");
        System.out.println("Upload URL: " + uploadUrl.substring(0, Math.min(100, uploadUrl.length())) + "...");
        System.out.println("File Size: " + videoBytes.length + " bytes");
        System.out.println("File Name: " + videoFile.getOriginalFilename());
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.PUT,
                request,
                String.class
            );
            
            System.out.println("===== Resposta Upload =====");
            System.out.println("Status: " + response.getStatusCode());
            
            if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.CREATED) {
                throw new RuntimeException("Falha no upload: " + response.getStatusCode());
            }
            
            System.out.println("✅ Upload concluído com sucesso!");
            
        } catch (Exception e) {
            System.err.println("Erro ao fazer upload do vídeo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao fazer upload: " + e.getMessage(), e);
        }
    }

    public String uploadVideoToTikTok(String accessToken, MultipartFile videoFile) throws IOException {
        Map<String, Object> initResponse = initializeUpload(accessToken, videoFile.getSize());
        
        String uploadUrl = (String) initResponse.get("upload_url");
        String publishId = (String) initResponse.get("publish_id");
        
        if (uploadUrl == null || publishId == null) {
            throw new RuntimeException("Resposta inválida do TikTok: upload_url ou publish_id ausente");
        }
        
        uploadVideoFile(uploadUrl, videoFile);
        
        System.out.println("===== Upload Completo =====");
        System.out.println("Publish ID: " + publishId);
        System.out.println("O vídeo foi enviado para sua caixa de entrada no TikTok!");
        
        return publishId;
    }
}
