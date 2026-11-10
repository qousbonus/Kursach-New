package util;
import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import model.Order;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class NewFeaturesTest {
    static Object invoke(Class<?> type,String name,Class<?>[] signature,Object... args) throws Exception {
        Method m=type.getDeclaredMethod(name,signature);m.setAccessible(true);
        try{return m.invoke(null,args);}catch(InvocationTargetException ex){
            if(ex.getCause() instanceof Exception cause)throw cause;
            if(ex.getCause() instanceof Error cause)throw cause;
            throw new AssertionError(ex.getCause());
        }
    }
    @BeforeEach void clear(){HistoryClient.clearSession();}
    @AfterEach void cleanup(){HistoryClient.clearSession();}
    static HistoryClient.Identity login(String token)throws IOException {
        HistoryClient.acceptLogin("{\"user\":{\"id\":\"11111111-2222-4333-8444-555555555555\"},\"access_token\":\""+token+"\"}");
        return HistoryClient.capture();
    }
    @Test void loginCreatesIdentity()throws Exception{var id=login("local");assertNotNull(id);assertTrue(HistoryClient.isCurrent(id));}
    @Test void logoutInvalidatesIdentity()throws Exception{var id=login("local");HistoryClient.clearSession();assertNull(HistoryClient.capture());assertFalse(HistoryClient.isCurrent(id));}
    @Test void reloginInvalidatesOldIdentity()throws Exception{var old=login("old");var next=login("new");assertNotSame(old,next);assertFalse(HistoryClient.isCurrent(old));assertTrue(HistoryClient.isCurrent(next));}
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"not-json","[]","{}","{\"user\":{}}","{\"user\":{\"id\":\"invalid\"},\"access_token\":\"x\"}"})
    void invalidLogin(String json){assertThrows(IOException.class,()->HistoryClient.acceptLogin(json));assertNull(HistoryClient.capture());}
    @Test void bearerHeaderWithoutSending()throws Exception{var id=login("local");var r=HistoryClient.authorize(id,HttpRequest.newBuilder(URI.create("https://example.invalid/"))).GET().build();assertEquals("Bearer local",r.headers().firstValue("Authorization").orElseThrow());}
    @Test void staleAuthorizationRejected()throws Exception{var old=login("old");login("new");assertThrows(IOException.class,()->HistoryClient.authorize(old,HttpRequest.newBuilder(URI.create("https://example.invalid/"))));}
    @ParameterizedTest @ValueSource(longs={0,-1,Long.MIN_VALUE})
    void invalidProductBeforeNetwork(long id)throws Exception{var session=login("local");assertThrows(IllegalArgumentException.class,()->HistoryClient.record(session,id));}
    @ParameterizedTest @ValueSource(ints={-1,0,101,Integer.MAX_VALUE})
    void invalidPageBeforeNetwork(int limit)throws Exception{var session=login("local");assertThrows(IllegalArgumentException.class,()->HistoryClient.page(session,limit,null));}

    @Nested class ThemeStorage {
        @TempDir Path home;String oldHome;
        @BeforeEach void setup(){oldHome=System.getProperty("user.home");System.setProperty("user.home",home.toString());}
        @AfterEach void restore(){if(oldHome==null)System.clearProperty("user.home");else System.setProperty("user.home",oldHome);}
        Path file(){return home.resolve(".protein-store/appearance.properties");}
        boolean load()throws Exception{return (Boolean)invoke(ThemeManager.class,"load",new Class<?>[0]);}
        void save(boolean dark)throws Exception{invoke(ThemeManager.class,"save",new Class<?>[]{boolean.class},dark);}
        void write(String s)throws Exception{Files.createDirectories(file().getParent());Files.writeString(file(),s);}
        @Test void absentUsesLight()throws Exception{assertFalse(load());}
        @ParameterizedTest @CsvSource({"DARK,true","LIGHT,false","OTHER,false","dark,false"})
        void readsSettings(String value,boolean expected)throws Exception{write("theme="+value);assertEquals(expected,load());}
        @Test void missingThemeUsesLight()throws Exception{write("other=value");assertFalse(load());}
        @Test void corruptSettingsUseLight()throws Exception{write("theme="+(char)92+"uZZZZ");assertFalse(load());}
        @Test void savesDark()throws Exception{save(true);assertTrue(load());Properties p=new Properties();try(var in=Files.newInputStream(file())){p.load(in);}assertEquals("DARK",p.getProperty("theme"));}
        @Test void replacementLeavesNoTempFile()throws Exception{save(true);save(false);assertFalse(load());try(var entries=Files.list(file().getParent())){assertEquals(1L,entries.count());}}
        @Test void invalidDirectoryRaisesIo()throws Exception{Files.writeString(home.resolve(".protein-store"),"file");assertThrows(IOException.class,()->save(true));}
    }
    static String stockState(Integer stock,int threshold,boolean active)throws Exception{
        Class<?> row=Class.forName("util.LowStockFeature$Row");
        var c=row.getDeclaredConstructor(long.class,String.class,Integer.class,int.class,boolean.class);c.setAccessible(true);
        return (String)invoke(LowStockFeature.class,"state",new Class<?>[]{row},c.newInstance(7L,"Test",stock,threshold,active));
    }
    @Test void nullStockIsUnknown()throws Exception{assertEquals("Остаток неизвестен",stockState(null,5,true));}
    @Test void hiddenTakesPriority()throws Exception{assertEquals("Скрыт",stockState(null,5,false));assertEquals("Скрыт",stockState(0,5,false));}
    @ParameterizedTest @CsvSource({"0,5,Нет в наличии","1,5,Нужно пополнить","5,5,Нужно пополнить","6,5,Достаточно","0,0,Нет в наличии","1,0,Достаточно"})
    void stockBoundaries(int stock,int threshold,String expected)throws Exception{assertEquals(expected,stockState(stock,threshold,true));}
    @ParameterizedTest @CsvSource({"0,5","-1,5","1,-1","0,-1","1,-2147483648"})
    void invalidThresholdBeforeNetwork(long product,int threshold){assertThrows(IllegalArgumentException.class,()->invoke(LowStockFeature.class,"save",new Class<?>[]{HistoryClient.Identity.class,long.class,int.class},null,product,threshold));}

    @ParameterizedTest @CsvSource({"1000,0,1000.00","1000,15,850.00","1000,100,0.00","1000,-1,1000.00","1000,101,0.00","0.05,10,0.05","0.01,50,0.01","0,50,0.00","19.99,15,16.99"})
    void popularPrice(String price,int discount,String expected){assertEquals(new BigDecimal(expected),PopularProductsFeature.discountedPrice(new BigDecimal(price),discount));}
    @Test void popularUnknownStock(){assertEquals("Остаток не указан",PopularProductsFeature.stockLabel(null));}
    @Test void popularZeroStock(){assertEquals("Нет в наличии",PopularProductsFeature.stockLabel(0));}
    @ParameterizedTest @CsvSource({"1,В наличии: 1 шт.","42,В наличии: 42 шт."})
    void popularPositiveStock(int stock,String expected){assertEquals(expected,PopularProductsFeature.stockLabel(stock));}

    @ParameterizedTest @CsvSource({"pending,Ожидание","confirmed,Подтверждён","processing,Собирается","shipped,В доставке","delivered,Доставлен","cancelled,Отменён"})
    void statusLabels(String status,String expected){assertEquals(expected,OrderTrackingClient.name(status));Order o=new Order();o.setStatus(status);assertEquals(expected,o.getStatusDisplay());}
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"unknown","SHIPPED"})
    void unknownStatus(String status){assertEquals("Неизвестен",OrderTrackingClient.name(status));}
    @Test void immutableStatuses(){assertEquals(List.of("pending","confirmed","processing","shipped","delivered","cancelled"),OrderTrackingClient.STATUSES);assertThrows(UnsupportedOperationException.class,()->OrderTrackingClient.STATUSES.add("other"));}
    @ParameterizedTest @ValueSource(strings={"2026-01-02T12:30:00Z","2026-01-02T12:30:00+04:00"})
    void offsetTime(String value){assertEquals(OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),OrderTrackingClient.localTime(value));}
    @Test void localTime(){assertEquals(LocalDateTime.of(2026,1,2,12,30),OrderTrackingClient.localTime("2026-01-02T12:30:00"));}
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","bad-time","2026-99-99"})
    void invalidTime(String value){assertNull(OrderTrackingClient.localTime(value));}
    @Test void snapshotUpdatesOnlyTrackingFields(){Order o=new Order();o.setId(20);o.setUserId(7);o.setTotalAmount(2500);o.setDeliveryAddress("Test address");OrderTrackingClient.apply(o,new OrderTrackingClient.Snapshot(20,7,"shipped","2026-01-02T10:00:00","2026-01-02T11:30:00"));assertEquals("shipped",o.getStatus());assertEquals(LocalDateTime.of(2026,1,2,10,0),o.getCreatedAt());assertEquals(LocalDateTime.of(2026,1,2,11,30),o.getUpdatedAt());assertEquals(20,o.getId());assertEquals(7,o.getUserId());assertEquals(2500.0,o.getTotalAmount(),0.000001);assertEquals("Test address",o.getDeliveryAddress());}
    @Test void nullSnapshotTimes(){Order o=new Order();o.setCreatedAt(LocalDateTime.now());o.setUpdatedAt(LocalDateTime.now());OrderTrackingClient.apply(o,new OrderTrackingClient.Snapshot(20,7,"pending",null,null));assertNull(o.getCreatedAt());assertNull(o.getUpdatedAt());}
    @ParameterizedTest @ValueSource(longs={0,-1,Long.MIN_VALUE})
    void invalidOrderId(long id){assertThrows(IllegalArgumentException.class,()->OrderTrackingClient.details(new OrderTrackingClient.Context(null,7,false),id));}
    static Object change(boolean staff,long user,long owner,String status,String target)throws Exception{return invoke(OrderTrackingClient.class,"change",new Class<?>[]{OrderTrackingClient.Context.class,OrderTrackingClient.Snapshot.class,String.class},new OrderTrackingClient.Context(null,user,staff),new OrderTrackingClient.Snapshot(20,owner,status,null,null),target);}
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"other","SHIPPED"})
    void invalidTarget(String value){assertThrows(IOException.class,()->change(true,7,7,"pending",value));}
    @Test void cannotConfirmForeignOrder(){assertThrows(IOException.class,()->change(false,7,8,"shipped","delivered"));}
    @ParameterizedTest @ValueSource(strings={"pending","confirmed","processing","delivered","cancelled"})
    void cannotConfirmUnshipped(String status){assertThrows(IOException.class,()->change(false,7,7,status,"delivered"));}
    @Test void cannotCancelFromTracking(){assertThrows(IOException.class,()->change(false,7,7,"shipped","cancelled"));}
    @Test void staffNoOpWithoutNetwork()throws Exception{var s=(OrderTrackingClient.Snapshot)change(true,7,8,"confirmed","confirmed");assertEquals("confirmed",s.status());assertEquals(8,s.userId());}
    @Test void absentContextNotCurrent(){assertFalse(OrderTrackingClient.current(null));assertFalse(OrderTrackingClient.current(new OrderTrackingClient.Context(null,7,false)));}
}
