package util;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public final class HistoryClient {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static volatile Identity current;
    private HistoryClient() { }
    public static final class Identity {
        private final String uid, token;
        private Identity(String uid, String token) { this.uid = uid; this.token = token; }
    }
    public record Entry(long productId, String name, String viewedAt, boolean available,
                        String imageUrl, double price, int discount) { }
    public record Page(List<Entry> items, boolean hasMore) { }
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
    static HttpRequest.Builder authorize(Identity identity, HttpRequest.Builder builder) throws IOException {
        if (!isCurrent(identity)) throw new IOException("Сессия изменилась. Войдите повторно.");
        return builder.header("Authorization", "Bearer " + identity.token);
    }
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
        if (response.statusCode() == 401) throw new IOException("Сессия истекла. Войдите повторно.");
        if (response.statusCode() >= 300) throw new IOException("Ошибка истории: HTTP " + response.statusCode());
        return response.body();
    }
    public static void record(Identity identity, long productId) throws Exception {
        if (productId <= 0) throw new IllegalArgumentException("Invalid product id");
        JsonObject body = new JsonObject();
        body.addProperty("auth_user_id", identity.uid);
        body.addProperty("product_id", productId);
        request(identity, "POST", "on_conflict=auth_user_id,product_id", body.toString());
    }
    private static String text(JsonObject obj, String field, String fallback) {
        return obj != null && obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : fallback;
    }
    public static Page page(Identity identity, int limit, Entry before) throws Exception {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Invalid page size");
        String query = "select=product_id,last_viewed_at,products(name,is_available,image_url,price,discount_percent)"
            + "&auth_user_id=eq." + identity.uid + "&order=last_viewed_at.desc,product_id.desc&limit=" + (limit + 1);
        if (before != null) {
            String stamp = URLEncoder.encode(before.viewedAt(), StandardCharsets.UTF_8);
            query += "&or=(last_viewed_at.lt." + stamp + ",and(last_viewed_at.eq." + stamp + ",product_id.lt." + before.productId() + "))";
        }
        List<Entry> rows = new ArrayList<>();
        for (JsonElement element : JsonParser.parseString(request(identity, "GET", query, null)).getAsJsonArray()) {
            JsonObject row = element.getAsJsonObject();
            JsonElement nested = row.get("products");
            JsonObject p = nested != null && nested.isJsonObject() ? nested.getAsJsonObject() : null;
            rows.add(new Entry(row.get("product_id").getAsLong(), text(p, "name", "Товар недоступен"),
                row.get("last_viewed_at").getAsString(), Boolean.parseBoolean(text(p, "is_available", "false")),
                text(p, "image_url", ""), Double.parseDouble(text(p, "price", "0")),
                Integer.parseInt(text(p, "discount_percent", "0"))));
        }
        return new Page(List.copyOf(rows.subList(0, Math.min(limit, rows.size()))), rows.size() > limit);
    }
    public static List<Entry> list(Identity identity) throws Exception { return page(identity, 50, null).items(); }
    public static void clear(Identity identity) throws Exception { request(identity, "DELETE", "auth_user_id=eq." + identity.uid, null); }
}
