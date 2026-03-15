package repository;

import model.PromoCode;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class PromoCodeRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private static final String SELECT =
            "select=id,code,discount_percent,max_uses,used_count,is_active,expires_at";

    private PromoCode map(String obj) {
        PromoCode pc = new PromoCode();
        pc.setId(Json.lng(obj, "id"));
        pc.setCode(Json.str(obj, "code"));
        pc.setDiscountPercent(Json.i(obj, "discount_percent"));
        pc.setMaxUses(Json.i(obj, "max_uses"));
        pc.setUsedCount(Json.i(obj, "used_count"));
        pc.setActive(Json.bool(obj, "is_active"));
        pc.setExpiresAt(Json.dt(obj, "expires_at"));
        return pc;
    }

    public List<PromoCode> findAll() throws Exception {
        String json = db.get("promo_codes", SELECT + "&order=id.desc");
        List<PromoCode> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        return list;
    }

    public PromoCode findByCode(String code) throws Exception {
        String json = db.get("promo_codes", SELECT + "&code=eq." + SupabaseClient.encode(code) + "&limit=1");
        List<String> rows = Json.array(json);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public void save(PromoCode pc) throws Exception {
        String expires = pc.getExpiresAt() != null
                ? Json.esc(pc.getExpiresAt().toString())
                : "null";
        String body = "{"
                + "\"code\":" + Json.esc(pc.getCode().toUpperCase()) + ","
                + "\"discount_percent\":" + pc.getDiscountPercent() + ","
                + "\"max_uses\":" + pc.getMaxUses() + ","
                + "\"used_count\":0,"
                + "\"is_active\":" + pc.isActive() + ","
                + "\"expires_at\":" + expires
                + "}";
        var resp = db.post("promo_codes", body);
        if (resp.statusCode() >= 400) throw new Exception(resp.body());
    }

    public void update(PromoCode pc) throws Exception {
        String expires = pc.getExpiresAt() != null
                ? Json.esc(pc.getExpiresAt().toString())
                : "null";
        String body = "{"
                + "\"code\":" + Json.esc(pc.getCode().toUpperCase()) + ","
                + "\"discount_percent\":" + pc.getDiscountPercent() + ","
                + "\"max_uses\":" + pc.getMaxUses() + ","
                + "\"is_active\":" + pc.isActive() + ","
                + "\"expires_at\":" + expires
                + "}";
        db.patch("promo_codes", "id=eq." + pc.getId(), body);
    }

    public void delete(long id) throws Exception {
        db.delete("promo_codes", "id=eq." + id);
    }

    public void setActive(long id, boolean active) throws Exception {
        db.patch("promo_codes", "id=eq." + id, "{\"is_active\":" + active + "}");
    }

    public void incrementUsed(long id) throws Exception {
        db.rpc("increment_promo_used", "{\"promo_id\":" + id + "}");
    }
}
