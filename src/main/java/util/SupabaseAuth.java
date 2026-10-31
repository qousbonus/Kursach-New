package util;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class SupabaseAuth {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final SupabaseConfig cfg = SupabaseConfig.getInstance();

    public AuthResult signIn(String email, String password) {
        HistoryClient.clearSession();
        return authenticate("/auth/v1/token?grant_type=password", email, password, true);
    }
    public AuthResult signUp(String email, String password) {
        return authenticate("/auth/v1/signup", email, password, false);
    }
    private AuthResult authenticate(String path, String email, String password, boolean login) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        try {
            var response = HTTP.send(HttpRequest.newBuilder(URI.create(cfg.getUrl() + path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .header("apikey", cfg.getAnonKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || (!login && response.statusCode() == 201)) {
                if (login) HistoryClient.acceptLogin(response.body());
                return new AuthResult(true, null);
            }
            return new AuthResult(false, login ? "Неверный email или пароль" : "Ошибка регистрации. Проверьте данные и доступность email.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AuthResult(false, "Запрос авторизации прерван");
        } catch (Exception e) {
            if (login) HistoryClient.clearSession();
            return new AuthResult(false, "Не удалось выполнить авторизацию. Проверьте подключение и повторите попытку.");
        }
    }
    public static class AuthResult {
        public final boolean success;
        public final String error;
        AuthResult(boolean success, String error) { this.success = success; this.error = error; }
    }
}
