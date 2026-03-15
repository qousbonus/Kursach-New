package repository;

import model.Order;
import model.OrderItem;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private static final String ORDER_SELECT =
            "select=id,user_id,total_amount,discount_amount,promo_code,status," +
                    "delivery_address,payment_method,note";

    private Order mapOrder(String obj) {
        Order o = new Order();
        o.setId(Json.lng(obj, "id"));
        o.setUserId(Json.lng(obj, "user_id"));
        o.setTotalAmount(Json.dbl(obj, "total_amount"));
        o.setDiscountAmount(Json.dbl(obj, "discount_amount"));
        o.setPromoCode(Json.str(obj, "promo_code"));
        o.setStatus(Json.str(obj, "status"));
        o.setDeliveryAddress(Json.str(obj, "delivery_address"));
        o.setPaymentMethod(Json.str(obj, "payment_method"));
        o.setNote(Json.str(obj, "note"));
        return o;
    }

    private void enrichUsernames(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        try {
            String ids = orders.stream()
                    .map(o -> String.valueOf(o.getUserId()))
                    .distinct()
                    .collect(Collectors.joining(","));
            String json = db.get("users", "select=id,username&id=in.(" + ids + ")");
            Map<Long, String> names = new HashMap<>();
            for (String row : Json.array(json)) {
                names.put(Json.lng(row, "id"), Json.str(row, "username"));
            }
            for (Order o : orders) {
                o.setUsername(names.getOrDefault(o.getUserId(), "Пользователь #" + o.getUserId()));
            }
        } catch (Exception ignored) {
        }
    }

    private OrderItem mapItem(String obj) {
        OrderItem item = new OrderItem();
        item.setId(Json.lng(obj, "id"));
        item.setOrderId(Json.lng(obj, "order_id"));
        item.setProductId(Json.lng(obj, "product_id"));
        item.setQuantity(Json.i(obj, "quantity"));
        item.setPrice(Json.dbl(obj, "price"));

        int pi = obj.indexOf("\"products\"");
        if (pi >= 0) {
            int ob = obj.indexOf('{', pi);
            int cb = obj.indexOf('}', ob);
            if (ob >= 0 && cb > ob) {
                item.setProductName(Json.str(obj.substring(ob, cb + 1), "name"));
            }
        }
        return item;
    }

    public List<Order> findByUser(long userId) throws Exception {
        String json = db.get("orders", ORDER_SELECT + "&user_id=eq." + userId + "&order=id.desc");
        List<Order> list = new ArrayList<>();
        for (String row : Json.array(json)) {
            Order o = mapOrder(row);
            o.setItems(findItemsByOrder(o.getId()));
            list.add(o);
        }
        enrichUsernames(list);
        return list;
    }

    public List<Order> findAll() throws Exception {
        String json = db.get("orders", ORDER_SELECT + "&order=id.desc");
        List<Order> list = new ArrayList<>();
        for (String row : Json.array(json)) {
            Order o = mapOrder(row);
            o.setItems(findItemsByOrder(o.getId()));
            list.add(o);
        }
        enrichUsernames(list);
        return list;
    }

    public List<OrderItem> findItemsByOrder(long orderId) throws Exception {
        String json = db.get("order_items",
                "select=id,order_id,product_id,quantity,price,products(name)" +
                        "&order_id=eq." + orderId);
        List<OrderItem> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(mapItem(row));
        return list;
    }

    public long createOrder(Order o) throws Exception {
        String body = "{"
                + "\"user_id\":" + o.getUserId() + ","
                + "\"total_amount\":" + o.getTotalAmount() + ","
                + "\"discount_amount\":" + o.getDiscountAmount() + ","
                + "\"promo_code\":" + Json.esc(o.getPromoCode()) + ","
                + "\"status\":\"confirmed\","
                + "\"delivery_address\":" + Json.esc(o.getDeliveryAddress()) + ","
                + "\"payment_method\":" + Json.esc(o.getPaymentMethod()) + ","
                + "\"note\":" + Json.esc(o.getNote())
                + "}";
        var resp = db.post("orders", body);
        if (resp.statusCode() >= 400) throw new Exception(resp.body());
        List<String> rows = Json.array(resp.body());
        if (!rows.isEmpty()) {
            long id = Json.lng(rows.get(0), "id");
            o.setId(id);
            return id;
        }
        return -1;
    }

    public void addOrderItem(long orderId, long productId, int quantity, double price) throws Exception {
        String body = "{"
                + "\"order_id\":" + orderId + ","
                + "\"product_id\":" + productId + ","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price
                + "}";
        var resp = db.post("order_items", body);
        if (resp.statusCode() >= 400) throw new Exception(resp.body());
    }

    public void updateStatus(long orderId, String status) throws Exception {
        db.patch("orders", "id=eq." + orderId, "{\"status\":" + Json.esc(status) + "}");
    }
}
