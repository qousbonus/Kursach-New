package util;

import com.google.gson.*;
import model.Order;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Authenticated tracking API. Call network methods outside the JavaFX thread. */
public final class OrderTrackingClient {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public static final List<String> STATUSES=List.of("pending","confirmed","processing","shipped","delivered","cancelled");
    private OrderTrackingClient() { }
    public record Context(HistoryClient.Identity identity,long userId,boolean staff) { }
    public record Snapshot(long id,long userId,String status,String createdAt,String updatedAt) { }
    public record Event(long id,String previous,String status,String kind,String at) { }
    public static Context context() throws IOException {
        Session s=Session.getInstance();var identity=HistoryClient.capture();
        if(!s.isLoggedIn()||identity==null)throw new IOException("Войдите повторно для доступа к заказам.");
        return new Context(identity,s.getCurrentUser().getId(),s.isManager()||s.isAdmin());
    }
    public static boolean current(Context c){return c!=null&&HistoryClient.isCurrent(c.identity());}
    public static String name(String status){return switch(status==null?"":status){
        case "pending"->"Ожидание";case "confirmed"->"Подтверждён";case "processing"->"Собирается";
        case "shipped"->"В доставке";case "delivered"->"Доставлен";case "cancelled"->"Отменён";default->"Неизвестен";};}
    public static LocalDateTime localTime(String value){
        if(value==null||value.isBlank())return null;
        try{return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();}
        catch(RuntimeException e){try{return LocalDateTime.parse(value);}catch(RuntimeException ignored){return null;}}
    }
    private static String text(JsonObject row,String key){return row.has(key)&&!row.get(key).isJsonNull()?row.get(key).getAsString():null;}
    private static Snapshot map(JsonObject row){return new Snapshot(row.get("id").getAsLong(),row.get("user_id").getAsLong(),text(row,"status"),text(row,"created_at"),text(row,"updated_at"));}
    public static void apply(Order order,Snapshot snapshot){order.setStatus(snapshot.status());order.setCreatedAt(localTime(snapshot.createdAt()));order.setUpdatedAt(localTime(snapshot.updatedAt()));}
    private static JsonArray request(Context c,String endpoint,String method,String body) throws Exception {
        SupabaseConfig cfg=SupabaseConfig.getInstance();
        var b=HttpRequest.newBuilder(URI.create(cfg.getUrl()+"/rest/v1/"+endpoint)).timeout(Duration.ofSeconds(20))
            .header("apikey",cfg.getAnonKey()).header("Content-Type","application/json");
        HistoryClient.authorize(c.identity(),b);
        if(method.equals("PATCH"))b.header("Prefer","return=representation");
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response;
        try{response=HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString());}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
        if(!current(c))throw new IOException("Сессия изменилась. Войдите повторно.");
        if(response.statusCode()==401)throw new IOException("Сессия истекла. Войдите повторно.");
        if(response.statusCode()>=300)throw new IOException("Ошибка запроса заказа: HTTP "+response.statusCode());
        JsonElement parsed=JsonParser.parseString(response.body());
        if(!parsed.isJsonArray())throw new IOException("Некорректный ответ сервера заказов.");
        return parsed.getAsJsonArray();
    }
    public static Snapshot details(Context c,long id) throws Exception {
        if(id<=0)throw new IllegalArgumentException("Invalid order id");
        String q="orders?select=id,user_id,status,created_at,updated_at&id=eq."+id+"&limit=1"+(c.staff()?"":"&user_id=eq."+c.userId());
        JsonArray rows=request(c,q,"GET",null);
        if(rows.size()!=1)throw new IOException("Заказ не найден или недоступен.");
        return map(rows.get(0).getAsJsonObject());
    }
    public static List<Event> events(Context c,long id) throws Exception {
        details(c,id);List<Event> result=new ArrayList<>();long after=0;
        while(true){
            JsonArray rows=request(c,"order_status_history?select=id,previous_status,status,event_kind,changed_at&order_id=eq."+id+"&order=id.asc&limit=200&id=gt."+after,"GET",null);
            for(JsonElement value:rows){JsonObject row=value.getAsJsonObject();result.add(new Event(row.get("id").getAsLong(),text(row,"previous_status"),text(row,"status"),text(row,"event_kind"),text(row,"changed_at")));}
            if(rows.size()<200)break;
            long next=result.get(result.size()-1).id();if(next<=after)throw new IOException("Некорректная страница журнала.");after=next;
        }
        result.sort(Comparator.comparing((Event e)->OffsetDateTime.parse(e.at()).toInstant()).thenComparingLong(Event::id));
        return List.copyOf(result);
    }
    public static Snapshot change(Context c,long id,String target) throws Exception {return change(c,details(c,id),target);}
    public static Snapshot changeExpected(Context c,long id,String expected,String target) throws Exception {
        Snapshot actual=details(c,id);
        if(!Objects.equals(expected,actual.status()))throw new IOException("Статус уже изменился. Обновите список заказов.");
        return change(c,actual,target);
    }
    private static Snapshot change(Context c,Snapshot actual,String target) throws Exception {
        if(target==null||!STATUSES.contains(target))throw new IOException("Недопустимый статус заказа.");
        if(!c.staff()&&!(actual.userId()==c.userId()&&"shipped".equals(actual.status())&&"delivered".equals(target)))
            throw new IOException("Покупатель может только подтвердить получение своего отправленного заказа.");
        if(Objects.equals(actual.status(),target))return actual;
        String statusFilter=actual.status()==null?"is.null":"eq."+URLEncoder.encode(actual.status(),StandardCharsets.UTF_8);
        String timeFilter=actual.updatedAt()==null?"is.null":"eq."+URLEncoder.encode(actual.updatedAt(),StandardCharsets.UTF_8);
        String q="orders?select=id,user_id,status,created_at,updated_at&id=eq."+actual.id()+"&status="+statusFilter+"&updated_at="+timeFilter
            +(c.staff()?"":"&user_id=eq."+c.userId());
        JsonObject body=new JsonObject();body.addProperty("status",target);
        JsonArray rows=request(c,q,"PATCH",body.toString());
        if(rows.size()!=1)throw new IOException("Заказ изменился или обновление не выполнено. Обновите данные.");
        Snapshot result=map(rows.get(0).getAsJsonObject());
        if(result.id()!=actual.id()||!target.equals(result.status()))throw new IOException("Сервер не подтвердил новый статус.");
        return result;
    }
}
