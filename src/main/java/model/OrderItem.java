package model;

public class OrderItem {
    private long id;
    private long orderId;
    private long productId;
    private String productName;
    private int quantity;
    private double price;

    public OrderItem() {
    }

    public OrderItem(long productId, String productName, int quantity, double price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public double getSubtotal() {
        return price * quantity;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long v) {
        this.productId = v;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String v) {
        this.productName = v;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
