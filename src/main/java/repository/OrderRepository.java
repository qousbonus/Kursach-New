package repository;

import model.Order;
import model.OrderItem;
import util.Json;
import util.SupabaseClient;
import util.OrderTrackingClient;
import java.util.*;
import java.util.stream.Collectors;

public class OrderRepository {
    private final SupabaseClient db=SupabaseClient.getInstance();
    private static final String ORDER_SELECT="select=id,user_id,total_amount,discount_amount,promo_code,status,delivery_address,payment_method,note,created_at,updated_at";
    private Order mapOrder(String obj){
        Order o=new Order();o.setId(Json.lng(obj,"id"));o.setUserId(Json.lng(obj,"user_id"));
        o.setTotalAmount(Json.dbl(obj,"total_amount"));o.setDiscountAmount(Json.dbl(obj,"discount_amount"));
        o.setPromoCode(Json.str(obj,"promo_code"));o.setStatus(Json.str(obj,"status"));
        o.setDeliveryAddress(Json.str(obj,"delivery_address"));o.setPaymentMethod(Json.str(obj,"payment_method"));o.setNote(Json.str(obj,"note"));
        o.setCreatedAt(OrderTrackingClient.localTime(Json.str(obj,"created_at")));
        o.setUpdatedAt(OrderTrackingClient.localTime(Json.str(obj,"updated_at")));return o;
    }
    private void enrichUsernames(List<Order> orders){
        if(orders.isEmpty())return;
        try{
            String ids=orders.stream().map(o->String.valueOf(o.getUserId())).distinct().collect(Collectors.joining(","));
            Map<Long,String> names=new HashMap<>();
            for(String row:Json.array(db.get("users","select=id,username&id=in.("+ids+")")))names.put(Json.lng(row,"id"),Json.str(row,"username"));
            for(Order o:orders)o.setUsername(names.getOrDefault(o.getUserId(),"Пользователь #"+o.getUserId()));
        }catch(Exception ignored){ }
    }
    private OrderItem mapItem(String obj){
        OrderItem item=new OrderItem();item.setId(Json.lng(obj,"id"));item.setOrderId(Json.lng(obj,"order_id"));
        item.setProductId(Json.lng(obj,"product_id"));item.setQuantity(Json.i(obj,"quantity"));item.setPrice(Json.dbl(obj,"price"));
        int pi=obj.indexOf("\"products\"");if(pi>=0){int ob=obj.indexOf('{',pi),cb=obj.indexOf('}',ob);if(ob>=0&&cb>ob)item.setProductName(Json.str(obj.substring(ob,cb+1),"name"));}
        return item;
    }
    public List<Order> findByUser(long userId)throws Exception{return fetchOrders(ORDER_SELECT+"&user_id=eq."+userId+"&order=id.desc");}
    public List<Order> findAll()throws Exception{return fetchOrders(ORDER_SELECT+"&order=id.desc");}
    private List<Order> fetchOrders(String query)throws Exception{
        List<Order> list=new ArrayList<>();
        for(String row:Json.array(db.get("orders",query))){Order o=mapOrder(row);o.setItems(findItemsByOrder(o.getId()));list.add(o);}
        enrichUsernames(list);return list;
    }
    public List<OrderItem> findItemsByOrder(long orderId)throws Exception{
        List<OrderItem> list=new ArrayList<>();
        for(String row:Json.array(db.get("order_items","select=id,order_id,product_id,quantity,price,products(name)&order_id=eq."+orderId)))list.add(mapItem(row));
        return list;
    }
    public long createOrder(Order o)throws Exception{
        String body="{\"user_id\":"+o.getUserId()+",\"total_amount\":"+o.getTotalAmount()+",\"discount_amount\":"+o.getDiscountAmount()
            +",\"promo_code\":"+Json.esc(o.getPromoCode())+",\"status\":\"confirmed\",\"delivery_address\":"+Json.esc(o.getDeliveryAddress())
            +",\"payment_method\":"+Json.esc(o.getPaymentMethod())+",\"note\":"+Json.esc(o.getNote())+"}";
        var resp=db.post("orders",body);if(resp.statusCode()>=400)throw new Exception(resp.body());
        List<String> rows=Json.array(resp.body());if(rows.isEmpty())throw new Exception("Сервер не вернул созданный заказ.");
        Order saved=mapOrder(rows.get(0));o.setId(saved.getId());o.setStatus(saved.getStatus());o.setCreatedAt(saved.getCreatedAt());o.setUpdatedAt(saved.getUpdatedAt());return o.getId();
    }
    public void addOrderItem(long orderId,long productId,int quantity,double price)throws Exception{
        String body="{\"order_id\":"+orderId+",\"product_id\":"+productId+",\"quantity\":"+quantity+",\"price\":"+price+"}";
        var resp=db.post("order_items",body);if(resp.statusCode()>=400)throw new Exception(resp.body());
    }
    public void updateStatus(long orderId,String status)throws Exception{
        OrderTrackingClient.change(OrderTrackingClient.context(),orderId,status);
    }
}
