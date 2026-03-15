package repository;

import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    public void log(long userId, String action, String tableName, String details) {
        try {
            String body = "{"
                    + "\"user_id\":" + userId + ","
                    + "\"action\":" + Json.esc(action) + ","
                    + "\"table_name\":" + Json.esc(tableName) + ","
                    + "\"details\":" + Json.esc(details)
                    + "}";
            db.post("system_logs", body);
        } catch (Exception ignored) {
        }
    }

    public void log(String action, String details) {
        try {
            String body = "{"
                    + "\"action\":" + Json.esc(action) + ","
                    + "\"details\":" + Json.esc(details)
                    + "}";
            db.post("system_logs", body);
        } catch (Exception ignored) {
        }
    }

    public List<Object[]> findAll() throws Exception {
        String json = db.get("system_logs",
                "select=id,user_id,action,table_name,details,created_at" +
                        "&order=created_at.desc&limit=500");

        List<Object[]> rows = new ArrayList<>();
        List<String> rawRows = Json.array(json);

        List<Long> userIds = rawRows.stream()
                .map(r -> Json.lng(r, "user_id"))
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> usernames = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                String ids = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                String usersJson = db.get("users", "select=id,username&id=in.(" + ids + ")");
                for (String u : Json.array(usersJson)) {
                    usernames.put(Json.lng(u, "id"), Json.str(u, "username"));
                }
            } catch (Exception ignored) {
            }
        }

        for (String row : rawRows) {
            long uid = Json.lng(row, "user_id");
            String username = uid > 0 ? usernames.getOrDefault(uid, "User#" + uid) : null;
            rows.add(new Object[]{
                    Json.lng(row, "id"),
                    username,
                    Json.str(row, "action"),
                    Json.str(row, "table_name"),
                    Json.str(row, "details"),
                    Json.str(row, "created_at")
            });
        }
        return rows;
    }
}
