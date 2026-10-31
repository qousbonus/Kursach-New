package util;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Only the history endpoint uses these in-memory user credentials. */
public final class HistoryClient {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static volatile Identity current;
    private HistoryClient() { }

    public static final class Identity {
        private final String uid, token;
        private Identity(String uid, String token) { this.uid = uid; this.token = token; }
    }
    public record Entry(long productId, String name, String viewedAt, boolean available) { }

    public static void acceptLogin(String json) throws IOException {
        try {
            JsonObject result = JsonParser.parseString(json).getAsJsonObject();
            String uid = UUID.fromString(result.getAsJsonObject("user").get("id").getAsString()).toString();
            String token = result.get("access_token").getAsString();
            if (token.isBlank()) throw new IllegalArgumentException();
            current = new Identity(uid, token);
        } catch (RuntimeException e) { throw new IOException("Invalid authentication response"); }
    }
    public static void clearSession() { current = null; }
    public static Identity capture() { return current; }
    public static boolean isCurrent(Identity identity) { return identity != null && current == identity; }

    private static String request(Identity identity, String method, String query, String body) throws Exception {
        if (!isCurrent(identity)) throw new IOException("Сессия изменилась. Войдите повторно.");
        SupabaseConfig cfg = SupabaseConfig.getInstance();
        var builder = HttpRequest.newBuilder(URI.create(cfg.getUrl() + "/rest/v1/product_view_history?" + query))
            .timeout(Duration.ofSeconds(20)).header("apikey", cfg.getAnonKey())
            .header("Authorization", "Bearer " + identity.token).header("Content-Type", "application/json");
        if (method.equals("POST")) builder.header("Prefer", "resolution=merge-duplicates,return=minimal");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (!isCurrent(identity)) throw new IOException("Сессия изменилась. Войдите повторно.");
        if (response.statusCode() == 401) throw new IOException("Сессия истекла. Выйдите из аккаунта и войдите повторно.");
        if (response.statusCode() >= 300) throw new IOException("Ошибка истории просмотров: HTTP " + response.statusCode());
        return response.body();
    }
    public static void record(Identity identity, long productId) throws Exception {
        if (productId <= 0) throw new IllegalArgumentException("Invalid product id");
        JsonObject body = new JsonObject();
        body.addProperty("auth_user_id", identity.uid);
        body.addProperty("product_id", productId);
        request(identity, "POST", "on_conflict=auth_user_id,product_id", body.toString());
    }
    public static List<Entry> list(Identity identity) throws Exception {
        String query = "select=product_id,last_viewed_at,products(name,is_available)&auth_user_id=eq." + identity.uid
            + "&order=last_viewed_at.desc,product_id.desc&limit=50";
        List<Entry> entries = new ArrayList<>();
        for (JsonElement element : JsonParser.parseString(request(identity, "GET", query, null)).getAsJsonArray()) {
            JsonObject row = element.getAsJsonObject();
            JsonElement nested = row.get("products");
            JsonObject product = nested != null && nested.isJsonObject() ? nested.getAsJsonObject() : null;
            String name = product == null ? "Товар недоступен" : product.get("name").getAsString();
            boolean available = product != null && product.has("is_available") && !product.get("is_available").isJsonNull()
                && product.get("is_available").getAsBoolean();
            entries.add(new Entry(row.get("product_id").getAsLong(), name, row.get("last_viewed_at").getAsString(), available));
        }
        return entries;
    }
    public static void clear(Identity identity) throws Exception {
        request(identity, "DELETE", "auth_user_id=eq." + identity.uid, null);
    }
}
