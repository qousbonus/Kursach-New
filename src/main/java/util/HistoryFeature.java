package util;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import model.Product;
import repository.ProductRepository;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class HistoryFeature {
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(256), task -> { Thread t = new Thread(task, "product-history"); t.setDaemon(true); return t; });
    private HistoryFeature() { }
    private static boolean buyer() {
        Session s = Session.getInstance();
        return s.isLoggedIn() && !s.isManager() && !s.isAdmin();
    }
    private static <T> void task(HistoryClient.Identity identity, Callable<T> work, Consumer<T> done, Consumer<String> fail) {
        try {
            WORK.execute(() -> {
                if (!HistoryClient.isCurrent(identity)) return;
                try {
                    T value = work.call();
                    Platform.runLater(() -> { if (HistoryClient.isCurrent(identity)) done.accept(value); });
                } catch (Exception e) {
                    Platform.runLater(() -> { if (HistoryClient.isCurrent(identity)) fail.accept("Не удалось выполнить запрос. Проверьте сеть; при истечении сессии войдите повторно."); });
                }
            });
        } catch (RejectedExecutionException e) { fail.accept("Очередь занята. Повторите позже."); }
    }
    private static String time(String raw) {
        try { return OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")); }
        catch (RuntimeException e) { return raw; }
    }
    private static Label label(String text, String style) {
        Label result = new Label(text); result.getStyleClass().add(style); result.setWrapText(true); return result;
    }
    private static String price(HistoryClient.Entry item) {
        if (!item.available()) return "Товар недоступен";
        int discount = Math.max(0, Math.min(100, item.discount()));
        return String.format("%.0f руб.", item.price() * (100 - discount) / 100.0);
    }
    private static Node picture(String url, double width, double height) {
        Label fallback = label("Нет фото", "label-muted");
        fallback.setVisible(url == null || url.isBlank());
        StackPane box = new StackPane(fallback);
        box.setMinSize(width, height); box.setPrefSize(width, height); box.setMaxSize(width, height);
        box.getStyleClass().add("product-image-box");
        if (url != null && !url.isBlank()) {
            try {
                Image image = new Image(url, width, height, true, true, true);
                ImageView view = new ImageView(image);
                view.setFitWidth(width); view.setFitHeight(height); view.setPreserveRatio(true);
                fallback.visibleProperty().bind(image.errorProperty());
                box.getChildren().add(view);
            } catch (IllegalArgumentException ignored) { fallback.setVisible(true); }
        }
        return box;
    }
    public static void record(Product product) {
        if (!buyer()) return;
        var identity = HistoryClient.capture();
        if (identity == null) return;
        long id = product.getId();
        task(identity, () -> { HistoryClient.record(identity, id); return true; }, ignored -> { },
            error -> AlertHelper.error("Просмотр не сохранён", error));
    }
    private static void open(HistoryClient.Identity identity, HistoryClient.Entry entry, Runnable beforeOpen,
                             Consumer<Boolean> busy, Consumer<String> status) {
        if (!entry.available()) { status.accept("Товар больше недоступен."); return; }
        busy.accept(true);
        task(identity, () -> new ProductRepository().findById(entry.productId()), product -> {
            busy.accept(false);
            if (product == null || !product.isAvailable()) { status.accept("Товар больше недоступен."); return; }
            beforeOpen.run();
            SceneManager.showProductDetail(product, null);
        }, error -> { busy.accept(false); status.accept(error); });
    }
    public static void install(Parent root) {
        if (!buyer() || root.lookup("#historySection") != null) return;
        Node anchor = root.lookup("#firstNameField");
        while (anchor != null && !anchor.getStyleClass().contains("profile-section-card")) anchor = anchor.getParent();
        if (anchor == null || !(anchor.getParent() instanceof VBox container)) return;
        var identity = HistoryClient.capture();
        HBox strip = new HBox(14);
        strip.setAlignment(Pos.TOP_LEFT);
        ScrollPane scroll = new ScrollPane(strip);
        scroll.setFitToWidth(false); scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(295); scroll.setMinHeight(310);
        Label status = label("Загрузка…", "label-muted");
        Button all = new Button("Посмотреть все просмотренные товары");
        all.getStyleClass().add("btn-outline");
        VBox section = new VBox(12, label("Недавно просмотренные товары", "heading-sm"), status, scroll, all);
        section.setId("historySection"); section.getStyleClass().add("profile-section-card");
        class Preview {
            boolean alive() {
                return HistoryClient.isCurrent(identity) && root.getScene() != null
                    && SceneManager.getStage().getScene() == root.getScene();
            }
            void load() {
                if (!alive()) { status.setText("Войдите повторно для доступа к истории."); return; }
                status.setText("Загрузка…");
                task(identity, () -> HistoryClient.page(identity, 8, null), page -> {
                    if (!alive()) return;
                    strip.getChildren().clear();
                    for (var entry : page.items()) {
                        Label name = label(entry.name(), "product-name-label");
                        name.setMinHeight(42); name.setMaxHeight(42);
                        Button button = new Button("Открыть"); button.setMaxWidth(Double.MAX_VALUE);
                        button.setDisable(!entry.available());
                        VBox card = new VBox(8, picture(entry.imageUrl(), 176, 105), name,
                            label(price(entry), "product-price-label"), label(time(entry.viewedAt()), "label-muted"), button);
                        card.setPadding(new Insets(12)); card.setMinWidth(204); card.setPrefWidth(204); card.setMaxWidth(204);
                        card.getStyleClass().add("product-card");
                        button.setOnAction(e -> open(identity, entry, () -> { }, section::setDisable, status::setText));
                        strip.getChildren().add(card);
                    }
                    status.setText(page.items().isEmpty() ? "Вы ещё не просматривали товары" : "Последние просмотренные товары");
                    scroll.setVisible(!page.items().isEmpty()); scroll.setManaged(!page.items().isEmpty());
                }, error -> { if (alive()) status.setText(error); });
            }
        }
        Preview preview = new Preview();
        all.setOnAction(e -> show(preview::load));
        container.getChildren().add(container.getChildren().indexOf(anchor), section);
        preview.load();
    }
    public static void show() { show(() -> { }); }
    private static void show(Runnable afterClose) {
        if (!buyer()) return;
        var identity = HistoryClient.capture();
        if (identity == null) { AlertHelper.info("История", "Войдите повторно для доступа к истории."); return; }
        Stage dialog = new Stage(); dialog.initOwner(SceneManager.getStage()); dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Все просмотренные товары");
        Label status = label("Загрузка…", "label-muted");
        ListView<HistoryClient.Entry> list = new ListView<>();
        list.setPlaceholder(new Label("История пуста"));
        Button refresh = new Button("Обновить"), clear = new Button("Очистить историю"), more = new Button("Показать ещё"), close = new Button("Закрыть");
        class Feed {
            HistoryClient.Entry cursor;
            boolean loading, hasMore;
            final Set<Long> ids = new HashSet<>();
            void busy(boolean value) {
                loading = value; refresh.setDisable(value); clear.setDisable(value); list.setDisable(value); more.setDisable(value || !hasMore);
            }
            void load(boolean reset, boolean erase) {
                if (loading) return;
                busy(true); status.setText("Загрузка…");
                var before = reset ? null : cursor;
                task(identity, () -> { if (erase) HistoryClient.clear(identity); return HistoryClient.page(identity, 20, before); }, page -> {
                    if (!dialog.isShowing()) return;
                    if (reset) { list.getItems().clear(); ids.clear(); }
                    for (var entry : page.items()) if (ids.add(entry.productId())) list.getItems().add(entry);
                    if (!page.items().isEmpty()) cursor = page.items().get(page.items().size() - 1);
                    else if (reset) cursor = null;
                    hasMore = page.hasMore();
                    status.setText("Загружено: " + list.getItems().size() + (hasMore ? ". Ниже есть ещё товары." : ". Вся история загружена."));
                    more.setVisible(hasMore); more.setManaged(hasMore); busy(false);
                }, error -> { if (dialog.isShowing()) { status.setText(error); busy(false); } });
            }
        }
        Feed feed = new Feed();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(HistoryClient.Entry entry, boolean empty) {
                super.updateItem(entry, empty); setText(null);
                if (empty || entry == null) { setGraphic(null); return; }
                VBox info = new VBox(7, label(entry.name(), "product-name-label"), label(price(entry), "product-price-label"), label("Просмотрено: " + time(entry.viewedAt()), "label-muted"));
                info.setMinWidth(0); info.setPrefWidth(400); HBox.setHgrow(info, Priority.ALWAYS);
                Button button = new Button("Открыть"); button.setDisable(!entry.available());
                button.setOnAction(e -> open(identity, entry, dialog::close, feed::busy, status::setText));
                HBox row = new HBox(16, picture(entry.imageUrl(), 110, 85), info, button);
                row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(12)); row.getStyleClass().add("order-card");
                row.prefWidthProperty().bind(list.widthProperty().subtract(40));
                setGraphic(row);
            }
        });
        refresh.setOnAction(e -> feed.load(true, false));
        more.setOnAction(e -> feed.load(false, false));
        clear.setOnAction(e -> { if (AlertHelper.confirm("Очистить историю", "Удалить всю вашу историю просмотров?")) feed.load(true, true); });
        close.setOnAction(e -> dialog.close());
        VBox root = new VBox(14, label("Все просмотренные товары", "heading-lg"), status, list, more, new HBox(10, refresh, clear, close));
        root.setPadding(new Insets(20)); VBox.setVgrow(list, Priority.ALWAYS);
        Scene scene = new Scene(root, 900, 620);
        var css = FxmlUtil.resolve("/css/style.css"); if (css != null) scene.getStylesheets().add(css.toExternalForm());
        ThemeManager.apply(scene); dialog.setScene(scene);
        dialog.setOnHidden(e -> Platform.runLater(afterClose));
        dialog.show(); feed.load(true, false);
    }
}
