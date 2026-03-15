package util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SupabaseClient {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static SupabaseClient instance;

    private final String baseUrl;
    private final String anonKey;

    public static SupabaseClient getInstance() {
        if (instance == null) {
            SupabaseConfig cfg = SupabaseConfig.getInstance();
            instance = new SupabaseClient(cfg.getUrl(), cfg.getAnonKey());
        }
        return instance;
    }

    public SupabaseClient(String baseUrl, String anonKey) {
        this.baseUrl = baseUrl;
        this.anonKey = anonKey;
    }

    private HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + anonKey);
    }

    public String get(String table, String query) throws Exception {
        String url = baseUrl + "/rest/v1/" + table + (query != null && !query.isBlank() ? "?" + query : "");
        HttpResponse<String> resp = HTTP.send(
                base(url).header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (resp.statusCode() >= 400)
            throw new Exception("HTTP " + resp.statusCode() + ": " + resp.body().substring(0, Math.min(300, resp.body().length())));
        return resp.body();
    }

    public HttpResponse<String> post(String table, String body) throws Exception {
        String url = baseUrl + "/rest/v1/" + table;
        return HTTP.send(
                base(url)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    public HttpResponse<String> upsert(String table, String body, String onConflict) throws Exception {
        String url = baseUrl + "/rest/v1/" + table + "?on_conflict=" + onConflict;
        HttpResponse<String> resp = HTTP.send(
                base(url)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "resolution=merge-duplicates,return=representation")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (resp.statusCode() >= 400)
            throw new Exception("HTTP " + resp.statusCode() + ": " + resp.body());
        return resp;
    }

    public HttpResponse<String> patch(String table, String query, String body) throws Exception {
        String url = baseUrl + "/rest/v1/" + table + (query != null && !query.isBlank() ? "?" + query : "");
        return HTTP.send(
                base(url)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    public HttpResponse<String> delete(String table, String query) throws Exception {
        String url = baseUrl + "/rest/v1/" + table + (query != null && !query.isBlank() ? "?" + query : "");
        return HTTP.send(base(url).DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    public String rpc(String function, String body) throws Exception {
        HttpResponse<String> resp = HTTP.send(
                base(baseUrl + "/rest/v1/rpc/" + function)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : "{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        return resp.body();
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // kept for backward compat with anything still calling it directly
    public static String jsonField(String json, String field) {
        return Json.str(json, field);
    }
}
