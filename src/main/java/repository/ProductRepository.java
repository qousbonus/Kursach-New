package repository;

import model.Product;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private static final String SELECT =
            "select=id,category_id,name,description,price,quantity_in_stock,image_url," +
                    "rating,reviews_count,discount_percent,is_available,brand," +
                    "categories(name,icon)";

    private Product map(String obj) {
        Product p = new Product();
        p.setId(Json.lng(obj, "id"));
        p.setCategoryId(Json.lng(obj, "category_id"));
        p.setName(Json.str(obj, "name"));
        p.setDescription(Json.str(obj, "description"));
        p.setPrice(Json.dbl(obj, "price"));
        p.setQuantityInStock(Json.i(obj, "quantity_in_stock"));
        p.setImageUrl(Json.str(obj, "image_url"));
        p.setRating(Json.dbl(obj, "rating"));
        p.setReviewsCount(Json.i(obj, "reviews_count"));
        p.setDiscountPercent(Json.i(obj, "discount_percent"));
        p.setAvailable(Json.bool(obj, "is_available"));
        p.setBrand(Json.str(obj, "brand"));
        p.setCreatedAt(Json.dt(obj, "created_at"));

        int ci = obj.indexOf("\"categories\"");
        if (ci >= 0) {
            int ob = obj.indexOf('{', ci);
            int cb = obj.indexOf('}', ob);
            if (ob >= 0 && cb > ob) {
                String cat = obj.substring(ob, cb + 1);
                p.setCategoryName(Json.str(cat, "name"));
                p.setCategoryIcon(Json.str(cat, "icon"));
            }
        }
        return p;
    }

    private List<Product> fetchList(String query) throws Exception {
        String json = db.get("products", query);
        List<Product> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        return list;
    }

    public List<Product> findAll() throws Exception {
        return fetchList(SELECT + "&order=name.asc");
    }

    public List<Product> findAvailable() throws Exception {
        return fetchList(SELECT + "&is_available=eq.true&order=name.asc");
    }

    public List<Product> findByCategory(long categoryId) throws Exception {
        return fetchList(SELECT + "&category_id=eq." + categoryId + "&is_available=eq.true&order=name.asc");
    }

    public List<Product> search(String query) throws Exception {
        String q = SupabaseClient.encode("%" + query + "%");
        String fullQuery = SELECT + "&is_available=eq.true&or=(name.ilike." + q
                + ",brand.ilike." + q + ",description.ilike." + q + ")&order=name.asc";
        return fetchList(fullQuery);
    }

    public Product findById(long id) throws Exception {
        String json = db.get("products", SELECT + "&id=eq." + id + "&limit=1");
        List<String> rows = Json.array(json);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    private String buildBody(Product p) {
        return "{"
                + "\"category_id\":" + p.getCategoryId() + ","
                + "\"name\":" + Json.esc(p.getName()) + ","
                + "\"description\":" + Json.esc(p.getDescription()) + ","
                + "\"price\":" + p.getPrice() + ","
                + "\"quantity_in_stock\":" + p.getQuantityInStock() + ","
                + "\"image_url\":" + Json.esc(p.getImageUrl()) + ","
                + "\"discount_percent\":" + p.getDiscountPercent() + ","
                + "\"is_available\":" + p.isAvailable() + ","
                + "\"brand\":" + Json.esc(p.getBrand())
                + "}";
    }

    public long save(Product p) throws Exception {
        var resp = db.post("products", buildBody(p));
        List<String> rows = Json.array(resp.body());
        if (!rows.isEmpty()) {
            long id = Json.lng(rows.get(0), "id");
            p.setId(id);
            return id;
        }
        return -1;
    }

    public void update(Product p) throws Exception {
        db.patch("products", "id=eq." + p.getId(), buildBody(p));
    }

    public void updateStock(long productId, int newStock) throws Exception {
        db.patch("products", "id=eq." + productId, "{\"quantity_in_stock\":" + newStock + "}");
    }

    public void deactivate(long id) throws Exception {
        db.patch("products", "id=eq." + id, "{\"is_available\":false}");
    }

    public void hardDelete(long id) throws Exception {
        db.delete("products", "id=eq." + id);
    }

    public void delete(long id) throws Exception {
        deactivate(id);
    }
}
