package util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Central appearance service; initialize and use on the JavaFX thread. */
public final class ThemeManager {
    private static final Set<Window> WINDOWS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Scene> SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Node> NODES = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean initialized, dark;
    private ThemeManager() { }

    public static void initialize(Window primary) {
        requireFx();
        if (!initialized) {
            dark = load();
            initialized = true;
            Window.getWindows().addListener((ListChangeListener<Window>) c -> {
                while (c.next()) if (c.wasAdded()) for (Window w : c.getAddedSubList()) watch(w);
            });
            for (Window w : Window.getWindows()) watch(w);
        }
        watch(primary);
    }
    public static boolean isDark() { return dark; }
    public static void setDark(boolean value) throws IOException {
        requireFx();
        if (dark == value) return;
        save(value);
        dark = value;
        for (Scene s : SCENES.toArray(new Scene[0])) decorate(s.getRoot());
    }
    private static void watch(Window w) {
        if (w == null || !WINDOWS.add(w)) return;
        w.sceneProperty().addListener((o, a, b) -> apply(b));
        apply(w.getScene());
    }
    public static void apply(Scene s) {
        requireFx();
        if (s == null) return;
        var resource = ThemeManager.class.getResource("/css/theme.css");
        if (resource == null) throw new IllegalStateException("Missing theme.css");
        String css = resource.toExternalForm();
        if (!s.getStylesheets().contains(css)) s.getStylesheets().add(css);
        if (SCENES.add(s)) s.rootProperty().addListener((o, a, b) -> prepare(b));
        prepare(s.getRoot());
    }
    private static void prepare(Parent root) {
        if (root == null) return;
        decorate(root);
        installPicker(root);
        observe(root);
    }
    private static void decorate(Parent root) {
        if (root == null) return;
        if (!root.getStyleClass().contains("theme-root")) root.getStyleClass().add("theme-root");
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(dark ? "theme-dark" : "theme-light");
    }
    private static void installPicker(Parent root) {
        if (root.lookup("#appearanceSettings") != null) return;
        Node anchor = root.lookup("#firstNameField");
        while (anchor != null && !anchor.getStyleClass().contains("profile-section-card")) anchor = anchor.getParent();
        if (anchor == null || !(anchor.getParent() instanceof VBox box)) return;
        Label title = new Label("Оформление приложения");
        title.getStyleClass().add("heading-sm");
        ComboBox<String> choice = new ComboBox<>();
        choice.setId("themeChoice");
        choice.getItems().setAll("Светлая", "Тёмная");
        choice.getSelectionModel().select(dark ? 1 : 0);
        choice.setMaxWidth(240);
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("text-error");
        boolean[] restoring = {false};
        choice.setOnAction(e -> {
            if (restoring[0]) return;
            error.setText("");
            try { setDark(choice.getSelectionModel().getSelectedIndex() == 1); }
            catch (IOException ex) {
                error.setText("Не удалось сохранить тему. Проверьте доступ к папке настроек.");
                restoring[0] = true;
                try { choice.getSelectionModel().select(dark ? 1 : 0); }
                finally { restoring[0] = false; }
            }
        });
        VBox card = new VBox(12, title, choice, error);
        card.setId("appearanceSettings");
        card.getStyleClass().add("profile-section-card");
        box.getChildren().add(box.getChildren().indexOf(anchor), card);
    }
    // Transitional adapter: layout and semantic accent colors are not rewritten.
    private static void observe(Node n) {
        if (!NODES.add(n)) return;
        normalize(n);
        n.styleProperty().addListener((o, a, b) -> normalize(n));
        if (n instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) observe(child);
            p.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
                while (c.next()) if (c.wasAdded()) for (Node child : c.getAddedSubList()) observe(child);
            });
        }
    }
    private static void normalize(Node n) {
        String old = n.getStyle();
        if (old == null || old.isBlank()) return;
        String s = old.replaceAll("(?i)(-fx-background-color\\s*:\\s*)#FFFFFF\\b", "$1-app-surface")
            .replaceAll("(?i)(-fx-background-color\\s*:\\s*)#F4F4F6\\b", "$1-app-bg")
            .replaceAll("(?i)(-fx-background-color\\s*:\\s*)#(?:F8F8FA|F8F8F8|FAFAFA)\\b", "$1-app-alt")
            .replaceAll("(?i)(-fx-text-fill\\s*:\\s*)#(?:111111|333333)\\b", "$1-app-text")
            .replaceAll("(?i)(-fx-text-fill\\s*:\\s*)#(?:555555|666666|777777)\\b", "$1-app-secondary")
            .replaceAll("(?i)(-fx-text-fill\\s*:\\s*)#(?:888888|999999|AAAAAA|BBBBBB|CCCCCC)\\b", "$1-app-muted");
        if (!old.equals(s)) n.setStyle(s);
    }
    private static Path settings() {
        return Path.of(System.getProperty("user.home"), ".protein-store", "appearance.properties");
    }
    private static boolean load() {
        try (InputStream in = Files.newInputStream(settings())) {
            Properties p = new Properties();
            p.load(in);
            return "DARK".equals(p.getProperty("theme"));
        } catch (IOException | SecurityException | IllegalArgumentException e) { return false; }
    }
    private static void save(boolean value) throws IOException {
        Path tmp = null;
        try {
            Path target = settings();
            Files.createDirectories(target.getParent());
            tmp = Files.createTempFile(target.getParent(), "appearance-", ".tmp");
            Properties p = new Properties();
            p.setProperty("theme", value ? "DARK" : "LIGHT");
            try (OutputStream out = Files.newOutputStream(tmp)) { p.store(out, "Appearance"); }
            try { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (SecurityException e) { throw new IOException("Cannot save appearance", e); }
        finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException | SecurityException ignored) { }
        }
    }
    private static void requireFx() {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("JavaFX thread required");
    }
}
