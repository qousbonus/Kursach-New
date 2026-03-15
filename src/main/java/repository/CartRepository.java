package repository;

import model.CartItem;
import model.Product;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    public List<CartItem> findByUser(long userId) {
        try {
            String json = db.get("cart_items",
                    "select=product_id,quantity," +
                            "products(id,name,price,discount_percent,image_url)" +
                            "&user_id=eq." + userId);

            List<CartItem> list = new ArrayList<>();
            for (String row : Json.array(json)) {
                Product p = extractProduct(row);
                if (p == null) {
                    continue;
                }
                int qty = Json.i(row, "quantity");
                if (qty > 0) list.add(new CartItem(p, qty));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void upsert(long userId, long productId, int quantity) {
        try {
            String body = "{"
                    + "\"user_id\":" + userId + ","
                    + "\"product_id\":" + productId + ","
                    + "\"quantity\":" + quantity
                    + "}";
            db.upsert("cart_items", body, "user_id,product_id");
        } catch (Exception ignored) {
        }
    }

    public void delete(long userId, long productId) {
        try {
            db.delete("cart_items", "user_id=eq." + userId + "&product_id=eq." + productId);
        } catch (Exception ignored) {
        }
    }

    public void deleteAll(long userId) {
        try {
            db.delete("cart_items", "user_id=eq." + userId);
        } catch (Exception ignored) {
        }
    }

    private Product extractProduct(String row) {
        try {
            int pi = row.indexOf("\"products\"");
            if (pi < 0) {
                return null;
            }
            int ob = row.indexOf('{', pi);
            if (ob < 0) {
                return null;
            }
            int depth = 0, cb = -1;
            for (int i = ob; i < row.length(); i++) {
                if (row.charAt(i) == '{') depth++;

                else if (row.charAt(i) == '}') {
                    depth--;
                    if (depth == 0) {
                        cb = i;
                        break;
                    }
                }
            }
            if (cb < 0) {
                return null;
            }
            String pObj = row.substring(ob, cb + 1);

            Product p = new Product();
            p.setId(Json.lng(pObj, "id"));
            p.setName(Json.str(pObj, "name"));
            p.setPrice(Json.dbl(pObj, "price"));
            p.setDiscountPercent(Json.i(pObj, "discount_percent"));
            p.setImageUrl(Json.str(pObj, "image_url"));
            return p;
        } catch (Exception e) {
            return null;
        }
    }
}
