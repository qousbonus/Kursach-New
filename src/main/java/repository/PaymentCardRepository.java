package repository;

import model.PaymentCard;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class PaymentCardRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private PaymentCard map(String obj) {
        PaymentCard c = new PaymentCard();
        c.setId(Json.lng(obj, "id"));
        c.setUserId(Json.lng(obj, "user_id"));
        c.setCardHolder(Json.str(obj, "card_holder"));
        c.setLastFour(Json.str(obj, "last_four"));
        c.setCardType(Json.str(obj, "card_type"));
        c.setExpiryMonth(Json.str(obj, "expiry_month"));
        c.setExpiryYear(Json.str(obj, "expiry_year"));
        return c;
    }

    public List<PaymentCard> findByUser(long userId) throws Exception {
        String json = db.get("payment_cards", "user_id=eq." + userId + "&order=id.asc");
        List<PaymentCard> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        return list;
    }

    public void save(PaymentCard card) throws Exception {
        String body = "{"
                + "\"user_id\":" + card.getUserId() + ","
                + "\"card_holder\":" + Json.esc(card.getCardHolder()) + ","
                + "\"last_four\":" + Json.esc(card.getLastFour()) + ","
                + "\"card_type\":" + Json.esc(card.getCardType()) + ","
                + "\"expiry_month\":" + Json.esc(card.getExpiryMonth()) + ","
                + "\"expiry_year\":" + Json.esc(card.getExpiryYear())
                + "}";
        db.post("payment_cards", body);
    }

    public void delete(long id) throws Exception {
        db.delete("payment_cards", "id=eq." + id);
    }
}
