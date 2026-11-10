package util;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import model.Product;
import repository.ProductRepository;
import java.io.IOException;
import java.math.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

/** Public catalog widget; only aggregate sales are requested. */
public final class PopularProductsFeature {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(16), job -> { Thread t = new Thread(job, "popular-products"); t.setDaemon(true); return t; });
    private PopularProductsFeature() { }
    private record Item(long id, String name, String brand, String image, BigDecimal price,
                        int discount, Integer stock, long sold) { }
    private static String text(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : "";
    }
    private static List<Item> fetch() throws Exception {
        SupabaseConfig cfg = SupabaseConfig.getInstance();
        var response = HTTP.send(HttpRequest.newBuilder(URI.create(cfg.getUrl() + "/rest/v1/rpc/get_popular_products"))
            .timeout(Duration.ofSeconds(20)).header("apikey", cfg.getAnonKey())
            .header("Authorization", "Bearer " + cfg.getAnonKey()).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"p_limit\":6}")).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) throw new IOException("Popular products HTTP " + response.statusCode());
        JsonElement parsed = JsonParser.parseString(response.body());
        if (!parsed.isJsonArray()) throw new IOException("Invalid popular products response");
        List<Item> rows = new ArrayList<>();
        for (JsonElement value : parsed.getAsJsonArray()) {
            JsonObject row = value.getAsJsonObject();
            Integer stock = row.get("quantity_in_stock").isJsonNull() ? null : row.get("quantity_in_stock").getAsInt();
            rows.add(new Item(row.get("product_id").getAsLong(), text(row, "name"), text(row, "brand"),
                text(row, "image_url"), row.get("price").getAsBigDecimal(), row.get("discount_percent").getAsInt(),
                stock, row.get("sold_quantity").getAsLong()));
        }
        return rows;
    }
    static BigDecimal discountedPrice(BigDecimal basePrice, int discountPercent) {
        int discount = Math.max(0, Math.min(100, discountPercent));
        return basePrice.multiply(BigDecimal.valueOf(100 - discount))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    static String stockLabel(Integer stock) {
        return stock == null ? "Остаток не указан" : stock == 0
            ? "Нет в наличии" : "В наличии: " + stock + " шт.";
    }
    private static Label label(String text, String style) {
        Label l = new Label(text); l.setWrapText(true); l.getStyleClass().add(style); return l;
    }
    private static Node picture(String url) {
        Label fallback = label("Нет фото", "label-muted"); fallback.setVisible(url.isBlank());
        StackPane box = new StackPane(fallback);
        box.setMinSize(176, 110); box.setPrefSize(176, 110); box.setMaxSize(176, 110);
        box.getStyleClass().add("product-image-box");
        if (!url.isBlank()) {
            try {
                Image image = new Image(url, 176, 110, true, true, true);
                ImageView view = new ImageView(image); view.setFitWidth(176); view.setFitHeight(110); view.setPreserveRatio(true);
                fallback.visibleProperty().bind(image.errorProperty()); box.getChildren().add(view);
            } catch (IllegalArgumentException e) { fallback.setVisible(true); }
        }
        return box;
    }
    private static <T> void submit(Callable<T> work, Consumer<T> done, Runnable fail) {
        try {
            WORK.execute(() -> {
                try { T value = work.call(); Platform.runLater(() -> done.accept(value)); }
                catch (Exception e) { Platform.runLater(fail); }
            });
        } catch (RejectedExecutionException e) { fail.run(); }
    }
    public static void install(Parent root) {
        Session session = Session.getInstance();
        if (session.isManager() || session.isAdmin() || root.lookup("#popularProductsSection") != null) return;
        Node grid = root.lookup("#productGrid");
        if (grid == null || !(grid.getParent() instanceof VBox container)) return;
        Label title = label("Популярные товары", "heading-md");
        Button refresh = new Button("Обновить");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(12, title, spacer, refresh); heading.setAlignment(Pos.CENTER_LEFT);
        Label status = label("Загрузка…", "label-muted");
        HBox cards = new HBox(14); cards.setAlignment(Pos.TOP_LEFT);
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToHeight(true); scroll.setFitToWidth(false); scroll.setMinWidth(0);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(345); scroll.setMinHeight(355);
        VBox section = new VBox(12, heading, label("По количеству проданных единиц в доставленных заказах за всё время", "label-muted"), status, scroll);
        section.setId("popularProductsSection"); section.getStyleClass().add("profile-section-card");
        VBox.setMargin(section, new Insets(0, 0, 24, 0));
        class Widget {
            boolean alive() { return root.getScene() != null && SceneManager.getStage().getScene() == root.getScene(); }
            void error(String message) { status.setText(message); status.getStyleClass().setAll("text-error"); }
            void open(long id) {
                section.setDisable(true);
                submit(() -> new ProductRepository().findById(id), product -> {
                    if (!alive()) return;
                    section.setDisable(false);
                    if (product == null || !product.isAvailable()) { error("Товар больше недоступен. Обновите список."); return; }
                    SceneManager.showProductDetail(product, null);
                }, () -> { if (alive()) { section.setDisable(false); error("Не удалось открыть товар. Повторите попытку."); } });
            }
            void load() {
                refresh.setDisable(true); status.setText("Загрузка…"); status.getStyleClass().setAll("label-muted");
                submit(PopularProductsFeature::fetch, rows -> {
                    if (!alive()) return;
                    refresh.setDisable(false); cards.getChildren().clear();
                    int rank = 0;
                    for (Item item : rows) {
                        Label name = label(item.name(), "product-name-label"); name.setMinHeight(42); name.setMaxHeight(42);
                        BigDecimal price = discountedPrice(item.price(), item.discount());
                        String stock = stockLabel(item.stock());
                        Button open = new Button("Открыть товар"); open.setMaxWidth(Double.MAX_VALUE); open.setOnAction(e -> open(item.id()));
                        VBox card = new VBox(7, label("№ " + (++rank), "label-accent"), picture(item.image()), name,
                            label(item.brand(), "label-muted"), label(price.stripTrailingZeros().toPlainString() + " руб.", "product-price-label"),
                            label("Продано: " + item.sold() + " шт.", "label-muted"), label(stock, "label-muted"), open);
                        card.setPadding(new Insets(12)); card.setMinWidth(204); card.setPrefWidth(204); card.setMaxWidth(204);
                        card.getStyleClass().add("product-card"); cards.getChildren().add(card);
                    }
                    status.setText(rows.isEmpty() ? "Пока нет доступных товаров с продажами в доставленных заказах." : "Лидеры продаж: " + rows.size());
                    scroll.setVisible(!rows.isEmpty()); scroll.setManaged(!rows.isEmpty());
                }, () -> { if (alive()) { refresh.setDisable(false); error("Не удалось загрузить популярные товары. Проверьте подключение и нажмите «Обновить»."); } });
            }
        }
        Widget widget = new Widget(); refresh.setOnAction(e -> widget.load());
        container.getChildren().add(0, section); widget.load();
    }
}
