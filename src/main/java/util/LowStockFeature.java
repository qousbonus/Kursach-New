package util;

import com.google.gson.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import model.Product;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class LowStockFeature {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(32), job -> { Thread t = new Thread(job,"stock-alerts"); t.setDaemon(true); return t; });
    private LowStockFeature() { }
    private record Row(long id,String name,Integer stock,int threshold,boolean active) { }
    private record Snapshot(List<Row> alerts,List<Row> products) { }
    private static JsonArray request(HistoryClient.Identity id,String path,String method,String body) throws Exception {
        SupabaseConfig cfg = SupabaseConfig.getInstance();
        var b = HttpRequest.newBuilder(URI.create(cfg.getUrl()+"/rest/v1/"+path))
            .timeout(java.time.Duration.ofSeconds(20)).header("apikey",cfg.getAnonKey()).header("Content-Type","application/json");
        HistoryClient.authorize(id,b);
        if (method.equals("PATCH")) b.header("Prefer","return=representation");
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        var r = HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString());
        if (!HistoryClient.isCurrent(id)) throw new IOException("Session changed");
        if (r.statusCode()>=300) throw new IOException("Stock HTTP "+r.statusCode());
        return JsonParser.parseString(r.body()).getAsJsonArray();
    }
    private static List<Row> read(HistoryClient.Identity id,boolean alerts) throws Exception {
        List<Row> rows = new ArrayList<>(); long after=0;
        while (true) {
            String path=(alerts?"low_stock_products":"products")+"?select=id,name,quantity_in_stock,low_stock_threshold"
                +(alerts?"":",is_available")+"&order=id.asc&limit=200&id=gt."+after;
            JsonArray page=request(id,path,"GET",null);
            for (JsonElement value:page) {
                JsonObject p=value.getAsJsonObject();
                Integer stock=p.get("quantity_in_stock").isJsonNull()?null:p.get("quantity_in_stock").getAsInt();
                boolean active=alerts||(!p.get("is_available").isJsonNull()&&p.get("is_available").getAsBoolean());
                rows.add(new Row(p.get("id").getAsLong(),p.get("name").getAsString(),stock,p.get("low_stock_threshold").getAsInt(),active));
            }
            if (page.size()<200) break;
            long next=rows.get(rows.size()-1).id();
            if (next<=after) throw new IOException("Invalid page cursor");
            after=next;
        }
        return rows;
    }
    private static Snapshot fetch(HistoryClient.Identity id) throws Exception {
        List<Row> alerts=read(id,true);
        alerts.sort(Comparator.comparingInt((Row r)->r.stock()).thenComparingLong(Row::id));
        return new Snapshot(List.copyOf(alerts),List.copyOf(read(id,false)));
    }
    private static void save(HistoryClient.Identity id,long product,int threshold) throws Exception {
        if (product<=0||threshold<0) throw new IllegalArgumentException();
        JsonObject body=new JsonObject(); body.addProperty("low_stock_threshold",threshold);
        JsonArray result=request(id,"products?id=eq."+product+"&select=id,low_stock_threshold","PATCH",body.toString());
        if (result.size()!=1||result.get(0).getAsJsonObject().get("id").getAsLong()!=product
            ||result.get(0).getAsJsonObject().get("low_stock_threshold").getAsInt()!=threshold) throw new IOException("Threshold not confirmed");
    }
    private static <T> void task(HistoryClient.Identity id,Callable<T> work,Consumer<T> done,Runnable fail) {
        try { WORK.execute(()->{
            if (!HistoryClient.isCurrent(id)) return;
            try { T result=work.call(); Platform.runLater(()->{if(HistoryClient.isCurrent(id))done.accept(result);}); }
            catch(Exception e){Platform.runLater(()->{if(HistoryClient.isCurrent(id))fail.run();});}
        }); } catch(RejectedExecutionException e){fail.run();}
    }
    private static String state(Row r) {
        if(!r.active())return "Скрыт";
        if(r.stock()==null)return "Остаток неизвестен";
        if(r.stock()==0)return "Нет в наличии";
        return r.stock()<=r.threshold()?"Нужно пополнить":"Достаточно";
    }
    @SuppressWarnings("unchecked")
    private static void updateStockColumn(Parent root,Snapshot snapshot) {
        Node n=root.lookup("#productsTable"); if(!(n instanceof TableView<?>))return;
        TableView<Product> table=(TableView<Product>)n;
        Map<Long,Row> rows=new HashMap<>(); snapshot.products().forEach(r->rows.put(r.id(),r));
        for(TableColumn<Product,?> col:table.getColumns())if("На складе".equals(col.getText())) {
            TableColumn<Product,Integer> stock=(TableColumn<Product,Integer>)col;
            stock.setCellFactory(c->new TableCell<>() {
                @Override protected void updateItem(Integer value,boolean empty) {
                    super.updateItem(value,empty); setText(null); setStyle("");
                    if(empty)return;
                    Product p=getTableRow()==null?null:getTableRow().getItem();
                    Row row=p==null?null:rows.get(p.getId()); Integer qty=row==null?value:row.stock();
                    setText(qty==null?"Не указан":qty.toString());
                    if(qty!=null) {
                        if(qty==0)setStyle("-fx-text-fill:-app-danger;");
                        else if(qty<=(row==null?5:row.threshold()))setStyle("-fx-text-fill:-app-accent;");
                    }
                }
            });
        }
        table.refresh();
    }
    public static void install(Parent root) {
        if(!Session.getInstance().isManager()||root.lookup("#stockAlertsSection")!=null)return;
        Node tab=root.lookup("#tabProducts");
        if(tab==null||!(tab.getParent() instanceof VBox parent))return;
        var id=HistoryClient.capture();
        Label title=new Label("Контроль остатков"); title.getStyleClass().add("heading-sm");
        Label status=new Label("Загрузка…"); status.setWrapText(true);
        Button refresh=new Button("Обновить"), settings=new Button("Уведомления и пороги");
        VBox banner=new VBox(8,title,status,new HBox(10,refresh,settings));
        banner.setId("stockAlertsSection"); banner.getStyleClass().add("profile-section-card");
        class Panel implements ChangeListener<Scene> {
            boolean loading;
            final Timeline timer=new Timeline(new KeyFrame(Duration.seconds(60),e->refresh()));
            boolean alive(){return HistoryClient.isCurrent(id)&&Session.getInstance().isManager()&&root.getScene()==SceneManager.getStage().getScene();}
            void refresh(){
                if(!alive()){status.setText("Войдите повторно для доступа к остаткам.");return;}
                if(loading)return; loading=true; refresh.setDisable(true);
                task(id,()->fetch(id),data->{
                    if(!alive())return; loading=false; refresh.setDisable(false);
                    long zero=data.alerts().stream().filter(r->r.stock()==0).count();
                    long unknown=data.products().stream().filter(r->r.active()&&r.stock()==null).count();
                    status.setText("Нет в наличии: "+zero+" | Низкий остаток: "+(data.alerts().size()-zero)+" | Неизвестный остаток: "+unknown);
                    status.getStyleClass().setAll(data.alerts().isEmpty()?"text-success":"text-error");
                    updateStockColumn(root,data);
                },()->{if(alive()){loading=false;refresh.setDisable(false);status.setText("Ошибка обновления. Данные могут быть устаревшими. Проверьте сеть и сессию.");status.getStyleClass().setAll("text-error");}});
            }
            @Override public void changed(ObservableValue<? extends Scene> o,Scene a,Scene b){
                if(b!=root.getScene()){timer.stop();SceneManager.getStage().sceneProperty().removeListener(this);}
            }
        }
        Panel panel=new Panel(); refresh.setOnAction(e->panel.refresh()); settings.setOnAction(e->show(id,panel::refresh));
        parent.getChildren().add(0,banner);
        SceneManager.getStage().sceneProperty().addListener(panel); panel.timer.setCycleCount(Animation.INDEFINITE); panel.timer.play(); panel.refresh();
    }
    private static void show(HistoryClient.Identity id,Runnable changed) {
        if(!Session.getInstance().isManager()||!HistoryClient.isCurrent(id)){AlertHelper.info("Остатки","Войдите повторно как менеджер.");return;}
        Stage dialog=new Stage(); dialog.initOwner(SceneManager.getStage()); dialog.initModality(Modality.WINDOW_MODAL); dialog.setTitle("Уведомления и пороги");
        ComboBox<String> mode=new ComboBox<>();mode.getItems().setAll("Нужно пополнить","Все товары — настройка порогов");mode.getSelectionModel().selectFirst();
        TextField search=new TextField();search.setPromptText("Поиск по названию");
        TableView<Row> table=new TableView<>();table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        String[] names={"ID","Товар","Остаток","Порог","Состояние"};
        for(int i=0;i<names.length;i++){
            final int index=i;TableColumn<Row,String> col=new TableColumn<>(names[i]);
            col.setCellValueFactory(d->{Row r=d.getValue();return new ReadOnlyStringWrapper(switch(index){case 0->Long.toString(r.id());case 1->r.name();case 2->r.stock()==null?"Не указан":r.stock().toString();case 3->Integer.toString(r.threshold());default->state(r);});});table.getColumns().add(col);
        }
        table.setPlaceholder(new Label("Нет подходящих товаров"));
        Spinner<Integer> threshold=new Spinner<>(0,Integer.MAX_VALUE,5);threshold.setEditable(true);threshold.setPrefWidth(140);
        Button save=new Button("Сохранить порог"),reload=new Button("Обновить"),close=new Button("Закрыть");
        Label status=new Label("Загрузка…");status.setWrapText(true);
        class Data {
            Snapshot snapshot=new Snapshot(List.of(),List.of());boolean loading;
            void filter(){List<Row> source=mode.getSelectionModel().getSelectedIndex()==0?snapshot.alerts():snapshot.products();String q=search.getText().trim().toLowerCase(Locale.ROOT);table.getItems().setAll(source.stream().filter(r->r.name().toLowerCase(Locale.ROOT).contains(q)).toList());}
            void busy(boolean value){loading=value;table.setDisable(value);mode.setDisable(value);search.setDisable(value);threshold.setDisable(value);save.setDisable(value);reload.setDisable(value);}
            void load(Row selected,Integer value){
                if(loading)return;busy(true);status.setText("Загрузка…");
                task(id,()->{if(selected!=null)LowStockFeature.save(id,selected.id(),value);return fetch(id);},data->{
                    if(!dialog.isShowing())return;snapshot=data;filter();busy(false);status.setText(selected==null?"Выберите товар для изменения порога. Неизвестный остаток не считается нулевым.":"Порог сохранён. Остаток не изменялся.");changed.run();
                },()->{if(dialog.isShowing()){busy(false);status.setText("Ошибка запроса. Если выполнялось сохранение, оно могло завершиться: обновите данные перед повтором. Проверьте сеть и сессию.");}});
            }
        }
        Data data=new Data();mode.setOnAction(e->data.filter());search.textProperty().addListener((o,a,b)->data.filter());
        table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null)threshold.getValueFactory().setValue(b.threshold());});
        save.setOnAction(e->{
            Row selected=table.getSelectionModel().getSelectedItem();if(selected==null){status.setText("Выберите товар.");return;}
            int value;try{value=Integer.parseInt(threshold.getEditor().getText().trim());if(value<0)throw new NumberFormatException();}
            catch(NumberFormatException ex){status.setText("Порог должен быть целым числом от 0 до 2147483647.");return;}
            data.load(selected,value);
        });
        reload.setOnAction(e->data.load(null,null));close.setOnAction(e->dialog.close());
        VBox root=new VBox(12,new HBox(12,mode,search),status,table,new HBox(10,new Label("Порог, шт.:"),threshold,save),new HBox(10,reload,close));root.setPadding(new Insets(20));VBox.setVgrow(table,Priority.ALWAYS);
        Scene scene=new Scene(root,980,620);var css=FxmlUtil.resolve("/css/style.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());ThemeManager.apply(scene);dialog.setScene(scene);
        dialog.setOnHidden(e->changed.run());dialog.show();data.load(null,null);
    }
}
