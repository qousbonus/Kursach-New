package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.Order;
import model.OrderItem;
import util.OrderTrackingClient;
import util.OrderTrackingFeature;
import util.SceneManager;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class OrderCardController implements Initializable {
    @FXML private Label orderIdLabel,statusLabel,dateLabel,totalLabel,addressLabel;
    @FXML private VBox itemsBox;
    @FXML private Button markReceivedBtn;
    private static final DateTimeFormatter FMT=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private Order order;
    private Runnable onStatusChanged;
    private Button trackingButton;
    @Override public void initialize(URL url,ResourceBundle rb){ }
    public void setOrder(Order order){setOrder(order,null);}
    public void setOrder(Order order,Runnable callback){this.order=order;this.onStatusChanged=callback;bind();}
    private boolean active(){return itemsBox.getScene()!=null&&SceneManager.getStage().getScene()==itemsBox.getScene();}
    private void bind(){
        orderIdLabel.setText("Заказ #"+order.getId());statusLabel.setText(order.getStatusDisplay());
        String status=order.getStatus();String css=status!=null&&OrderTrackingClient.STATUSES.contains(status)?"status-"+status:"status-pending";
        statusLabel.getStyleClass().setAll(css);
        dateLabel.setText(order.getCreatedAt()==null?"Дата неизвестна":order.getCreatedAt().format(FMT));
        dateLabel.setTooltip(new Tooltip("Дата создания в часовом поясе устройства. Для старых заказов может быть не зафиксирована."));
        totalLabel.setText(String.format("Итого: %.0f руб.",order.getFinalAmount()));addressLabel.setText(order.getDeliveryAddress()==null?"":order.getDeliveryAddress());
        itemsBox.getChildren().clear();
        if(order.getItems()!=null)for(OrderItem item:order.getItems()){
            Label l=new Label(item.getProductName()+"  x"+item.getQuantity()+"  —  "+String.format("%.0f руб.",item.getSubtotal()));l.getStyleClass().add("order-item-label");itemsBox.getChildren().add(l);
        }
        if(markReceivedBtn!=null){boolean shipped="shipped".equals(status);markReceivedBtn.setVisible(shipped);markReceivedBtn.setManaged(shipped);}
        if(trackingButton==null&&itemsBox.getParent() instanceof VBox root){
            trackingButton=new Button("Отследить заказ");trackingButton.getStyleClass().add("btn-outline");
            trackingButton.setOnAction(e->OrderTrackingFeature.show(order.getId(),snapshot->{if(active()){OrderTrackingClient.apply(order,snapshot);bind();}}));
            root.getChildren().add(trackingButton);
        }
    }
    @FXML public void handleMarkReceived(){
        if(order==null)return;
        OrderTrackingFeature.change(order,"delivered",value->{if(markReceivedBtn!=null)markReceivedBtn.setDisable(value);},()->{
            if(active()){bind();if(onStatusChanged!=null)onStatusChanged.run();}
        });
    }
}
