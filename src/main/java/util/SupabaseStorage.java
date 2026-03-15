package util;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class SupabaseStorage {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final String url;
    private final String anonKey;
    private final String bucket;

    public SupabaseStorage() {
        SupabaseConfig cfg = SupabaseConfig.getInstance();
        this.url = cfg.getUrl();
        this.anonKey = cfg.getAnonKey();
        this.bucket = cfg.getBucket();
    }

    public UploadResult upload(File file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getName();
            String contentType = detectContentType(file.getName());
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/storage/v1/object/" + bucket + "/" + fileName))
                    .header("Authorization", "Bearer " + anonKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String publicUrl = url + "/storage/v1/object/public/" + bucket + "/" + fileName;
                return new UploadResult(true, publicUrl, null);
            } else {
                return new UploadResult(false, null, "Ошибка загрузки: " + response.statusCode());
            }
        } catch (Exception e) {
            return new UploadResult(false, null, "Ошибка: " + e.getMessage());
        }
    }

    private String detectContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    public static class UploadResult {
        public final boolean success;
        public final String publicUrl;
        public final String error;

        UploadResult(boolean success, String publicUrl, String error) {
            this.success = success;
            this.publicUrl = publicUrl;
            this.error = error;
        }
    }
}
