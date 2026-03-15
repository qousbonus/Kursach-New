package repository;

import model.User;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private User map(String obj) {
        User u = new User();
        u.setId(Json.lng(obj, "id"));
        u.setEmail(Json.str(obj, "email"));
        u.setUsername(Json.str(obj, "username"));
        u.setFirstName(Json.str(obj, "first_name"));
        u.setLastName(Json.str(obj, "last_name"));
        u.setPhone(Json.str(obj, "phone"));
        u.setRole(Json.str(obj, "role"));
        u.setActive(Json.bool(obj, "is_active"));
        u.setAccountLocked(Json.bool(obj, "account_locked"));
        u.setLastLogin(Json.dt(obj, "last_login"));
        u.setCreatedAt(Json.dt(obj, "created_at"));
        return u;
    }

    public User findByUsername(String username) throws Exception {
        String json = db.get("users", "username=eq." + SupabaseClient.encode(username) + "&limit=1");
        List<String> rows = Json.array(json);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public User findByEmail(String email) throws Exception {
        String json = db.get("users", "email=eq." + SupabaseClient.encode(email) + "&limit=1");
        List<String> rows = Json.array(json);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public User findById(long id) throws Exception {
        String json = db.get("users", "id=eq." + id + "&limit=1");
        List<String> rows = Json.array(json);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public List<User> findAll() throws Exception {
        String json = db.get("users", "order=created_at.desc");
        List<User> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        return list;
    }

    public void save(User u) throws Exception {
        String body = "{"
                + "\"email\":" + Json.esc(u.getEmail()) + ","
                + "\"username\":" + Json.esc(u.getUsername()) + ","
                + "\"password_hash\":" + Json.esc("supabase_auth") + ","
                + "\"first_name\":" + Json.esc(u.getFirstName()) + ","
                + "\"last_name\":" + Json.esc(u.getLastName()) + ","
                + "\"phone\":" + Json.esc(u.getPhone()) + ","
                + "\"role\":" + Json.esc(u.getRole() == null ? "user" : u.getRole()) + ","
                + "\"is_active\":true,\"account_locked\":false"
                + "}";
        var resp = db.post("users", body);
        List<String> rows = Json.array(resp.body());
        if (!rows.isEmpty()) {
            u.setId(Json.lng(rows.get(0), "id"));
        }
    }

    public void updateProfile(User u) throws Exception {
        String body = "{"
                + "\"first_name\":" + Json.esc(u.getFirstName()) + ","
                + "\"last_name\":" + Json.esc(u.getLastName()) + ","
                + "\"phone\":" + Json.esc(u.getPhone())
                + "}";
        db.patch("users", "id=eq." + u.getId(), body);
    }

    public void updateEmail(long userId, String newEmail) throws Exception {
        db.patch("users", "id=eq." + userId, "{\"email\":" + Json.esc(newEmail) + "}");
    }

    public void setLocked(long userId, boolean locked) throws Exception {
        db.patch("users", "id=eq." + userId, "{\"account_locked\":" + locked + "}");
    }

    public void setActive(long userId, boolean active) throws Exception {
        db.patch("users", "id=eq." + userId, "{\"is_active\":" + active + "}");
    }

    public void updateLastLogin(long userId) throws Exception {
        db.patch("users", "id=eq." + userId,
                "{\"last_login\":\"" + java.time.OffsetDateTime.now() + "\"}");
    }

    public void logFailedAttempt(String email, String reason) throws Exception {
        String body = "{\"email\":" + Json.esc(email) + ",\"reason\":" + Json.esc(reason) + "}";
        db.post("failed_login_attempts", body);
    }

    public List<Object[]> getFailedAttempts() throws Exception {
        String json = db.get("failed_login_attempts",
                "order=created_at.desc&limit=200&select=id,email,ip_address,reason,created_at");
        List<Object[]> list = new ArrayList<>();
        for (String row : Json.array(json)) {
            list.add(new Object[]{
                    Json.lng(row, "id"),
                    Json.str(row, "email"),
                    Json.str(row, "ip_address"),
                    Json.str(row, "reason"),
                    Json.str(row, "created_at")
            });
        }
        return list;
    }
}
