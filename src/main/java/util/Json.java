package util;

import com.google.gson.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Json {

    private static final Gson GSON = new Gson();

    public static String str(String json, String key) {
        JsonElement el = get(json, key);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }

    public static long lng(String json, String key) {
        JsonElement el = get(json, key);
        if (el == null || el.isJsonNull()) return 0;
        try {
            return el.getAsLong();
        } catch (Exception e) {
            return 0;
        }
    }

    public static int i(String json, String key) {
        return (int) lng(json, key);
    }

    public static double dbl(String json, String key) {
        JsonElement el = get(json, key);
        if (el == null || el.isJsonNull()) return 0;
        try {
            return el.getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean bool(String json, String key) {
        JsonElement el = get(json, key);
        return el != null && !el.isJsonNull() && el.getAsBoolean();
    }

    public static LocalDateTime dt(String json, String key) {
        String v = str(json, key);
        if (v == null) return null;
        try {
            String clean = v.replace("Z", "").replace("+00:00", "");
            if (clean.length() > 19) clean = clean.substring(0, 19);
            return LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> array(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        try {
            JsonArray arr = JsonParser.parseString(json.trim()).getAsJsonArray();
            for (JsonElement el : arr) result.add(el.toString());
        } catch (Exception ignored) {
        }
        return result;
    }

    public static String esc(String s) {
        if (s == null) return "null";
        return GSON.toJson(s);
    }

    private static JsonElement get(String json, String key) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.get(key);
        } catch (Exception e) {
            return null;
        }
    }
}
