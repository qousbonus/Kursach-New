package model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private long id,userId;
    private String username,promoCode,status,deliveryAddress,paymentMethod,note;
    private double totalAmount,discountAmount;
    private LocalDateTime createdAt,updatedAt;
    private List<OrderItem> items;
    public Order(){ }
    public double getFinalAmount(){return totalAmount-discountAmount;}
    public String getStatusDisplay(){return switch(status==null?"":status){
        case "pending"->"Ожидание";case "confirmed"->"Подтверждён";case "processing"->"Собирается";
        case "shipped"->"В доставке";case "delivered"->"Доставлен";case "cancelled"->"Отменён";default->"Неизвестен";};}
    public long getId(){return id;}public void setId(long v){id=v;}
    public long getUserId(){return userId;}public void setUserId(long v){userId=v;}
    public String getUsername(){return username;}public void setUsername(String v){username=v;}
    public double getTotalAmount(){return totalAmount;}public void setTotalAmount(double v){totalAmount=v;}
    public double getDiscountAmount(){return discountAmount;}public void setDiscountAmount(double v){discountAmount=v;}
    public String getPromoCode(){return promoCode;}public void setPromoCode(String v){promoCode=v;}
    public String getStatus(){return status;}public void setStatus(String v){status=v;}
    public String getDeliveryAddress(){return deliveryAddress;}public void setDeliveryAddress(String v){deliveryAddress=v;}
    public String getPaymentMethod(){return paymentMethod;}public void setPaymentMethod(String v){paymentMethod=v;}
    public String getNote(){return note;}public void setNote(String v){note=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public List<OrderItem> getItems(){return items;}public void setItems(List<OrderItem> v){items=v;}
}
