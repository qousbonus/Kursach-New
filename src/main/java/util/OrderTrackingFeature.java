package util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.beans.property.ReadOnlyStringWrapper;
import model.Order;
import repository.OrderRepository;
import repository.LogRepository;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class OrderTrackingFeature {
    private static final DateTimeFormatter FMT=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final ThreadPoolExecutor WORK=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"order-tracking");t.setDaemon(true);return t;});
    private static final ThreadPoolExecutor LOGS=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"order-tracking-log");t.setDaemon(true);return t;},new ThreadPoolExecutor.DiscardPolicy());
    private OrderTrackingFeature(){ }
    private record ViewData(OrderTrackingClient.Snapshot order,List<OrderTrackingClient.Event> events){ }
    private static <T> void task(OrderTrackingClient.Context c,Callable<T> work,Consumer<T> done,Consumer<String> fail){
        try{WORK.execute(()->{
            if(!OrderTrackingClient.current(c))return;
            try{T value=work.call();Platform.runLater(()->{if(OrderTrackingClient.current(c))done.accept(value);});}
            catch(Exception e){String message=e instanceof IOException&&e.getMessage()!=null?e.getMessage():"Не удалось выполнить запрос. Проверьте подключение и повторите попытку.";Platform.runLater(()->{if(OrderTrackingClient.current(c))fail.accept(message);});}
        });}catch(RejectedExecutionException e){fail.accept("Очередь запросов занята. Повторите позже.");}
    }
    private static String date(String value){var t=OrderTrackingClient.localTime(value);return t==null?"Не зафиксирована":t.format(FMT);}
    private static Label label(String value,String style){Label l=new Label(value);l.setWrapText(true);l.getStyleClass().add(style);return l;}
    private static String eventTitle(OrderTrackingClient.Event e){
        if("snapshot".equals(e.kind()))return "Состояние на момент внедрения: "+OrderTrackingClient.name(e.status());
        if("created".equals(e.kind()))return "Заказ создан: "+OrderTrackingClient.name(e.status());
        return OrderTrackingClient.name(e.previous())+" → "+OrderTrackingClient.name(e.status());
    }
    public static void show(long id){show(id,s->{ });}
    public static void show(long id,Consumer<OrderTrackingClient.Snapshot> updated){
        OrderTrackingClient.Context c;
        try{c=OrderTrackingClient.context();}catch(IOException e){AlertHelper.info("Отслеживание",e.getMessage());return;}
        Stage dialog=new Stage();dialog.initOwner(SceneManager.getStage());dialog.initModality(Modality.WINDOW_MODAL);dialog.setTitle("Отслеживание заказа #"+id);
        Label current=label("Загрузка статуса…","heading-md"),dates=label("","label-muted"),message=label("","label-muted");
        ListView<OrderTrackingClient.Event> events=new ListView<>();events.setPlaceholder(new Label("Событий нет либо они недоступны этой учётной записи"));
        events.setCellFactory(v->new ListCell<>(){
            @Override protected void updateItem(OrderTrackingClient.Event e,boolean empty){
                super.updateItem(e,empty);setText(null);
                if(empty||e==null){setGraphic(null);return;}
                VBox card=new VBox(7,label(eventTitle(e),"heading-sm"),label(date(e.at()),"label-muted"));
                if("snapshot".equals(e.kind()))card.getChildren().add(label("Это дата фиксации состояния при внедрении, а не дата прежнего изменения статуса.","label-muted"));
                card.getStyleClass().add("order-card");card.prefWidthProperty().bind(events.widthProperty().subtract(40));setGraphic(card);
            }
        });
        Button refresh=new Button("Обновить"),close=new Button("Закрыть");
        class Loader{
            boolean loading;
            final Timeline timer=new Timeline(new KeyFrame(Duration.seconds(15),e->load()));
            void load(){
                if(!dialog.isShowing())return;
                if(!OrderTrackingClient.current(c)){dialog.close();return;}
                if(loading)return;loading=true;refresh.setDisable(true);message.setText("Загрузка…");
                task(c,()->new ViewData(OrderTrackingClient.details(c,id),OrderTrackingClient.events(c,id)),data->{
                    if(!dialog.isShowing())return;loading=false;refresh.setDisable(false);
                    current.setText("Текущий статус: "+OrderTrackingClient.name(data.order().status()));
                    dates.setText("Создан: "+date(data.order().createdAt())+"\nОбновлён: "+date(data.order().updatedAt())+"\nЧасовой пояс устройства: "+ZoneId.systemDefault());
                    events.getItems().setAll(data.events());message.setText("Событий: "+data.events().size()+". Автообновление — 15 секунд.");
                    updated.accept(data.order());
                },error->{if(dialog.isShowing()){loading=false;refresh.setDisable(false);message.setText(error+" Показанные ранее данные могут быть устаревшими.");}});
            }
        }
        Loader loader=new Loader();refresh.setOnAction(e->loader.load());close.setOnAction(e->dialog.close());
        VBox root=new VBox(12,label("Заказ #"+id,"heading-lg"),current,dates,message,events,new HBox(10,refresh,close));root.setPadding(new Insets(20));VBox.setVgrow(events,Priority.ALWAYS);
        Scene scene=new Scene(root,900,650);var css=FxmlUtil.resolve("/css/style.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());ThemeManager.apply(scene);dialog.setScene(scene);
        dialog.setOnHidden(e->loader.timer.stop());dialog.show();loader.timer.setCycleCount(Animation.INDEFINITE);loader.timer.play();loader.load();
    }
    public static void change(Order order,String target,Consumer<Boolean> busy,Runnable done){
        if(order==null||target==null){AlertHelper.info("Статус","Выберите заказ и статус.");return;}
        OrderTrackingClient.Context c;
        try{c=OrderTrackingClient.context();}catch(IOException e){AlertHelper.error("Статус",e.getMessage());return;}
        String question="Установить заказу #"+order.getId()+" статус «"+OrderTrackingClient.name(target)+"»?";
        if("cancelled".equals(target))question+="\nАвтоматический возврат остатков при отмене не настроен.";
        if(!AlertHelper.confirm("Подтверждение статуса",question))return;
        long id=order.getId();String expected=order.getStatus();busy.accept(true);
        task(c,()->{
            var result=OrderTrackingClient.changeExpected(c,id,expected,target);
            if(!Objects.equals(expected,target))LOGS.execute(()->new LogRepository().log(c.userId(),"ORDER_STATUS_UPDATED","orders","Order #"+id+" -> "+target));
            return result;
        },snapshot->{OrderTrackingClient.apply(order,snapshot);busy.accept(false);done.run();},error->{busy.accept(false);AlertHelper.error("Статус заказа",error);});
    }
    @SuppressWarnings("unchecked")
    public static void installManager(Parent root){
        if(!Session.getInstance().isManager()||root.lookup("#orderTrackingManagerControls")!=null)return;
        Node tableNode=root.lookup("#ordersTable"),comboNode=root.lookup("#newStatusCombo"),filterNode=root.lookup("#orderStatusFilter"),tabNode=root.lookup("#tabOrders");
        if(!(tableNode instanceof TableView<?>)||!(comboNode instanceof ComboBox<?>)||!(filterNode instanceof ComboBox<?>)||!(tabNode instanceof VBox tab)||!(comboNode.getParent() instanceof HBox actions))return;
        TableView<Order> table=(TableView<Order>)tableNode;ComboBox<String> combo=(ComboBox<String>)comboNode,filter=(ComboBox<String>)filterNode;
        Button apply=actions.getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElse(null);
        if(apply==null)return;
        Label message=label("Выберите заказ для просмотра истории или смены статуса.","label-muted");
        Button history=new Button("Отследить выбранный заказ"),refresh=new Button("Обновить заказы");
        VBox controls=new VBox(8,new HBox(10,history,refresh),message);controls.setId("orderTrackingManagerControls");
        StringConverter<String> converter=new StringConverter<>(){
            @Override public String toString(String code){return "ALL".equals(code)?"Все":OrderTrackingClient.name(code);}
            @Override public String fromString(String value){return OrderTrackingClient.STATUSES.stream().filter(s->s.equals(value)||OrderTrackingClient.name(s).equals(value)).findFirst().orElse(null);}
        };
        filter.setOnAction(null);filter.setConverter(converter);filter.getItems().setAll("ALL");filter.getItems().addAll(OrderTrackingClient.STATUSES);filter.setValue("ALL");
        combo.setConverter(converter);combo.getItems().setAll(OrderTrackingClient.STATUSES);combo.setValue("processing");
        class Manager{
            List<Order> all=new ArrayList<>(table.getItems());boolean loading;
            boolean alive(){return root.getScene()!=null&&SceneManager.getStage().getScene()==root.getScene()&&Session.getInstance().isManager();}
            void render(){String code=filter.getValue();table.getItems().setAll(all.stream().filter(o->"ALL".equals(code)||Objects.equals(code,o.getStatus())).toList());table.refresh();}
            void busy(boolean value){loading=value;table.setDisable(value);combo.setDisable(value);filter.setDisable(value);apply.setDisable(value);history.setDisable(value);refresh.setDisable(value);message.setText(value?"Выполняется запрос…":"");}
            void reload(){
                if(loading)return;OrderTrackingClient.Context c;
                try{c=OrderTrackingClient.context();}catch(IOException e){message.setText(e.getMessage());return;}
                busy(true);task(c,()->new OrderRepository().findAll(),rows->{if(alive()){all=new ArrayList<>(rows);render();busy(false);message.setText("Список заказов обновлён.");}},error->{if(alive()){busy(false);message.setText(error);}});
            }
        }
        Manager manager=new Manager();filter.setOnAction(e->manager.render());refresh.setOnAction(e->manager.reload());
        history.setOnAction(e->{Order order=table.getSelectionModel().getSelectedItem();if(order==null){message.setText("Выберите заказ.");return;}show(order.getId(),snapshot->{if(manager.alive()){OrderTrackingClient.apply(order,snapshot);manager.render();}});});
        apply.setOnAction(e->{Order order=table.getSelectionModel().getSelectedItem();if(order==null){message.setText("Выберите заказ.");return;}change(order,combo.getValue(),manager::busy,()->{if(manager.alive()){manager.render();message.setText("Текущий статус: "+order.getStatusDisplay());}});});
        TableColumn<Order,String> created=new TableColumn<>("Создан");created.setId("trackingCreatedColumn");created.setCellValueFactory(d->new ReadOnlyStringWrapper(d.getValue().getCreatedAt()==null?"Неизвестна":d.getValue().getCreatedAt().format(FMT)));table.getColumns().add(created);
        tab.getChildren().add(1,controls);manager.render();
    }
}
