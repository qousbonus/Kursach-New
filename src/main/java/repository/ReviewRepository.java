package repository;

import model.Review;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReviewRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private static final String SELECT = "select=id,product_id,user_id,rating,title,comment";

    private Review map(String obj) {
        Review r = new Review();
        r.setId(Json.lng(obj, "id"));
        r.setProductId(Json.lng(obj, "product_id"));
        r.setUserId(Json.lng(obj, "user_id"));
        r.setRating(Json.i(obj, "rating"));
        r.setTitle(Json.str(obj, "title"));
        r.setComment(Json.str(obj, "comment"));
        return r;
    }

    private void enrichUsernames(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return;
        }
        try {
            String ids = reviews.stream()
                    .map(rv -> String.valueOf(rv.getUserId()))
                    .distinct()
                    .collect(Collectors.joining(","));
            String json = db.get("users", "select=id,username&id=in.(" + ids + ")");
            Map<Long, String> names = new HashMap<>();
            for (String row : Json.array(json)) {
                names.put(Json.lng(row, "id"), Json.str(row, "username"));
            }
            for (Review rv : reviews) {
                rv.setUsername(names.getOrDefault(rv.getUserId(), "Пользователь"));
            }
        } catch (Exception ignored) {
        }
    }

    public List<Review> findByProduct(long productId) throws Exception {
        String json = db.get("reviews", SELECT + "&product_id=eq." + productId + "&order=id.desc");
        List<Review> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        enrichUsernames(list);
        return list;
    }

    public List<Review> findAll() throws Exception {
        String json = db.get("reviews", SELECT + "&order=id.desc");
        List<Review> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        enrichUsernames(list);
        return list;
    }

    public void save(Review r) throws Exception {
        String body = "{"
                + "\"product_id\":" + r.getProductId() + ","
                + "\"user_id\":" + r.getUserId() + ","
                + "\"rating\":" + r.getRating() + ","
                + "\"title\":" + Json.esc(r.getTitle()) + ","
                + "\"comment\":" + Json.esc(r.getComment())
                + "}";
        var resp = db.post("reviews", body);
        if (resp.statusCode() >= 400) throw new Exception(resp.body());
    }

    public void reject(long reviewId) throws Exception {
        db.delete("reviews", "id=eq." + reviewId);
    }

    public void syncProductStats(long productId) throws Exception {
        String json = db.get("reviews", "select=rating&product_id=eq." + productId);
        List<String> rows = Json.array(json);
        int count = rows.size();
        double avg = 0;
        if (count > 0) {
            int sum = 0;
            for (String row : rows) sum += Json.i(row, "rating");
            avg = Math.round((sum / (double) count) * 10.0) / 10.0;
        }
        db.patch("products", "id=eq." + productId, "{\"rating\":" + avg + ",\"reviews_count\":" + count + "}");
    }
}
