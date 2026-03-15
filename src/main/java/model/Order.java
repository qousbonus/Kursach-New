package model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private long id;
    private long userId;
    private String username;
    private double totalAmount;
    private double discountAmount;
    private String promoCode;
    private String status;
    private String deliveryAddress;
    private String paymentMethod;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItem> items;

    public Order() {
    }

    public double getFinalAmount() {
        return totalAmount - discountAmount;
    }

    public String getStatusDisplay() {
        return switch (status == null ? "" : status) {
            case "pending", "confirmed", "processing" -> "Ожидание";
            case "shipped" -> "Доставка";
            case "delivered" -> "Получено";
            case "cancelled" -> "Отменён";
            default -> status;
        };
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double v) {
        this.totalAmount = v;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double v) {
        this.discountAmount = v;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String v) {
        this.promoCode = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String v) {
        this.deliveryAddress = v;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String v) {
        this.paymentMethod = v;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        this.updatedAt = v;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> v) {
        this.items = v;
    }
}
