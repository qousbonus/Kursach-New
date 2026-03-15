package util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SupabaseAuth {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private final String url;
    private final String anonKey;

    public SupabaseAuth() {
        SupabaseConfig cfg = SupabaseConfig.getInstance();
        this.url = cfg.getUrl();
        this.anonKey = cfg.getAnonKey();
    }

    public AuthResult signIn(String email, String password) {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        try {
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url + "/auth/v1/token?grant_type=password"))
                            .header("Content-Type", "application/json")
                            .header("apikey", anonKey)
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (resp.statusCode() == 200) return new AuthResult(true, null);
            String rb = resp.body();
            String msg = "Неверный email или пароль";
            if (rb != null && rb.contains("User already registered")) msg = "Email уже используется";
            return new AuthResult(false, msg);
        } catch (Exception e) {
            return new AuthResult(false, "Ошибка сети: " + e.getMessage());
        }
    }

    public AuthResult signUp(String email, String password) {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        try {
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(url + "/auth/v1/signup"))
                            .header("Content-Type", "application/json")
                            .header("apikey", anonKey)
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (resp.statusCode() == 200 || resp.statusCode() == 201) return new AuthResult(true, null);
            String rb = resp.body();
            String msg = "Ошибка регистрации";
            if (rb != null && rb.contains("User already registered")) msg = "Email уже зарегистрирован";
            return new AuthResult(false, msg);
        } catch (Exception e) {
            return new AuthResult(false, "Ошибка сети: " + e.getMessage());
        }
    }

    public static class AuthResult {
        public final boolean success;
        public final String error;

        AuthResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }
}
