package service;

import model.CartItem;
import model.Order;
import model.PromoCode;
import repository.LogRepository;
import repository.OrderRepository;
import repository.PromoCodeRepository;
import util.Session;

import java.util.List;

public class OrderService {

    private final OrderRepository orderRepo = new OrderRepository();
    private final PromoCodeRepository promoRepo = new PromoCodeRepository();
    private final LogRepository logRepo = new LogRepository();

    public PromoCode validatePromo(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            PromoCode pc = promoRepo.findByCode(code.trim().toUpperCase());
            return (pc != null && pc.isValid()) ? pc : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String placeOrder(String address, String paymentMethod, String promoCode, String note) {
        Session session = Session.getInstance();
        if (!session.isLoggedIn()) return "Необходима авторизация";
        if (session.getCart().isEmpty()) return "Корзина пуста";
        if (address == null || address.isBlank()) return "Укажите адрес доставки";
        if (paymentMethod == null || paymentMethod.isBlank()) return "Укажите способ оплаты";

        try {
            List<CartItem> cartItems = session.getCart();
            double total = session.getCartTotal();
            PromoCode pc = null;
            double discount = 0;

            if (promoCode != null && !promoCode.isBlank()) {
                pc = validatePromo(promoCode);
                if (pc != null) discount = total * pc.getDiscountPercent() / 100.0;
            }

            Order order = new Order();
            order.setUserId(session.getCurrentUser().getId());
            order.setTotalAmount(total);
            order.setDiscountAmount(discount);
            order.setPromoCode(pc != null ? pc.getCode() : null);
            order.setDeliveryAddress(address);
            order.setPaymentMethod(paymentMethod);
            order.setNote(note);

            long orderId = orderRepo.createOrder(order);
            for (CartItem item : cartItems) {
                orderRepo.addOrderItem(orderId, item.getProduct().getId(),
                        item.getQuantity(), item.getProduct().getFinalPrice());
            }
            if (pc != null) promoRepo.incrementUsed(pc.getId());

            logRepo.log(session.getCurrentUser().getId(), "ORDER_PLACED", "orders",
                    "Order #" + orderId + " placed, total: " + (total - discount));
            session.clearCart();
            return null;

        } catch (Exception e) {
            return "Ошибка оформления заказа: " + e.getMessage();
        }
    }

    public List<Order> getUserOrders(long userId) {
        try {
            return orderRepo.findByUser(userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Order> getAllOrders() {
        try {
            return orderRepo.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public String updateStatus(long orderId, String status) {
        try {
            orderRepo.updateStatus(orderId, status);
            Session s = Session.getInstance();
            if (s.isLoggedIn())
                logRepo.log(s.getCurrentUser().getId(), "ORDER_STATUS_UPDATED", "orders",
                        "Order #" + orderId + " -> " + status);
            return null;
        } catch (Exception e) {
            return "Ошибка обновления статуса: " + e.getMessage();
        }
    }
}
