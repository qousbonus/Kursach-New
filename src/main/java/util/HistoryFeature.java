package util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import model.Product;
import repository.ProductRepository;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;

public final class HistoryFeature {
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(256), task -> { Thread t = new Thread(task, "product-history"); t.setDaemon(true); return t; });
    private HistoryFeature() { }
    private static boolean buyer() {
        Session s = Session.getInstance();
        return s.isLoggedIn() && !s.isManager() && !s.isAdmin();
    }
    public static void install(Parent root) {
        if (!buyer() || root.lookup("#historySection") != null) return;
        Node anchor = root.lookup("#firstNameField");
        while (anchor != null && !anchor.getStyleClass().contains("profile-section-card")) anchor = anchor.getParent();
        if (anchor == null || !(anchor.getParent() instanceof VBox box)) return;
        Label title = new Label("Недавно просмотренные товары");
        title.getStyleClass().add("heading-sm");
        Button open = new Button("Открыть историю просмотров");
        open.getStyleClass().add("btn-outline");
        open.setOnAction(e -> show());
        VBox card = new VBox(12, title, open);
        card.setId("historySection");
        card.getStyleClass().add("profile-section-card");
        box.getChildren().add(box.getChildren().indexOf(anchor), card);
    }
    public static void record(Product product) {
        if (!buyer()) return;
        var identity = HistoryClient.capture();
        if (identity == null) return;
        long id = product.getId();
        try {
            WORK.execute(() -> {
                if (!HistoryClient.isCurrent(identity)) return;
                try { HistoryClient.record(identity, id); }
                catch (Exception e) {
                    Platform.runLater(() -> {
                        if (HistoryClient.isCurrent(identity) && buyer())
                            AlertHelper.error("История просмотров", "Просмотр не сохранён. Проверьте подключение; при истечении сессии войдите повторно.");
                    });
                }
            });
        } catch (RejectedExecutionException e) {
            AlertHelper.error("История просмотров", "Очередь сохранения заполнена. Этот просмотр не сохранён.");
        }
    }
    private static String time(String raw) {
        try { return OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")); }
        catch (RuntimeException e) { return raw; }
    }
    public static void show() {
        if (!buyer()) return;
        var identity = HistoryClient.capture();
        if (identity == null) {
            AlertHelper.info("История просмотров", "Выйдите из аккаунта и войдите повторно для доступа к истории.");
            return;
        }
        Stage dialog = new Stage();
        dialog.initOwner(SceneManager.getStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("История просмотров");
        ListView<HistoryClient.Entry> list = new ListView<>();
        list.setPlaceholder(new Label("История пуста"));
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(HistoryClient.Entry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " — " + time(item.viewedAt()) + (item.available() ? "" : " (недоступен)"));
            }
        });
        Label status = new Label();
        status.setWrapText(true);
        Button refresh = new Button("Обновить");
        Button clear = new Button("Очистить историю");
        Button open = new Button("Открыть товар");
        Button close = new Button("Закрыть");
        HBox actions = new HBox(10, refresh, clear, open, close);
        class Loader {
            void busy(boolean value) { refresh.setDisable(value); clear.setDisable(value); open.setDisable(value); }
            void load(boolean erase) {
                busy(true); status.setText("Загрузка…");
                try {
                    WORK.execute(() -> {
                        try {
                            if (erase) HistoryClient.clear(identity);
                            List<HistoryClient.Entry> rows = HistoryClient.list(identity);
                            Platform.runLater(() -> {
                                if (!dialog.isShowing()) return;
                                if (!HistoryClient.isCurrent(identity)) { dialog.close(); return; }
                                list.getItems().setAll(rows);
                                status.setText("Последние " + rows.size() + " товаров (не более 50)");
                                busy(false);
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                if (!dialog.isShowing()) return;
                                if (!HistoryClient.isCurrent(identity)) { dialog.close(); return; }
                                status.setText("Не удалось загрузить или очистить историю. Проверьте сеть; если сессия истекла, войдите повторно.");
                                busy(false);
                            });
                        }
                    });
                } catch (RejectedExecutionException e) { status.setText("Очередь занята. Повторите позже."); busy(false); }
            }
        }
        Loader loader = new Loader();
        refresh.setOnAction(e -> loader.load(false));
        clear.setOnAction(e -> { if (AlertHelper.confirm("Очистка истории", "Удалить всю вашу историю просмотров?")) loader.load(true); });
        close.setOnAction(e -> dialog.close());
        open.setOnAction(e -> {
            var item = list.getSelectionModel().getSelectedItem();
            if (item == null) { status.setText("Выберите товар в списке."); return; }
            if (!item.available()) { status.setText("Товар больше недоступен в каталоге."); return; }
            loader.busy(true);
            try {
                WORK.execute(() -> {
                    try {
                        Product product = new ProductRepository().findById(item.productId());
                        Platform.runLater(() -> {
                            if (!dialog.isShowing()) return;
                            if (!HistoryClient.isCurrent(identity)) { dialog.close(); return; }
                            loader.busy(false);
                            if (product == null || !product.isAvailable()) { status.setText("Товар больше недоступен."); return; }
                            dialog.close();
                            SceneManager.showProductDetail(product, null);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            if (!dialog.isShowing()) return;
                            if (!HistoryClient.isCurrent(identity)) { dialog.close(); return; }
                            status.setText("Не удалось открыть товар. Повторите попытку."); loader.busy(false);
                        });
                    }
                });
            } catch (RejectedExecutionException ex) { status.setText("Очередь занята. Повторите позже."); loader.busy(false); }
        });
        VBox root = new VBox(14, new Label("История просмотров"), status, list, actions);
        root.setPadding(new Insets(20));
        VBox.setVgrow(list, Priority.ALWAYS);
        Scene scene = new Scene(root, 820, 520);
        var css = FxmlUtil.resolve("/css/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        ThemeManager.apply(scene);
        dialog.setScene(scene);
        dialog.show();
        loader.load(false);
    }
}
