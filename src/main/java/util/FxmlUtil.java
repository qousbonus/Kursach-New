package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public final class FxmlUtil {

    private FxmlUtil() {
    }

    public static URL resolve(String path) {
        String stripped = path.startsWith("/") ? path.substring(1) : path;

        URL url = FxmlUtil.class.getClassLoader().getResource(stripped);
        if (url != null) {
            return url;
        }

        url = Thread.currentThread().getContextClassLoader().getResource(stripped);
        if (url != null) {
            return url;
        }

        url = ClassLoader.getSystemResource(stripped);
        if (url != null) {
            return url;
        }

        return FxmlUtil.class.getResource(path);
    }

    public static FXMLLoader loader(String path) throws IOException {
        URL url = resolve(path);
        if (url == null) {
            throw new IOException("FXML not found: " + path);
        }
        return new FXMLLoader(url);
    }

    public static Parent load(String path) throws IOException {
        return FxmlUtil.loader(path).load();
    }
}
