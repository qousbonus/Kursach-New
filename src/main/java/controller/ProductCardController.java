package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Product;
import util.AlertHelper;
import util.Session;

public class ProductCardController {

    @FXML
    private ImageView productImage;
    @FXML
    private Label nameLabel;
    @FXML
    private Label brandLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label oldPriceLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label ratingLabel;
    @FXML
    private Label stockLabel;
    @FXML
    private Button addToCartBtn;

    private Product product;
    private CatalogController catalog;

    public void setProduct(Product product, CatalogController catalog) {
        this.product = product;
        this.catalog = catalog;
        bind();
    }

    private void bind() {
        nameLabel.setText(product.getName());
        brandLabel.setText(product.getBrand() != null ? product.getBrand() : "");

        if (categoryLabel != null) {
            String icon = product.getCategoryIcon() != null ? product.getCategoryIcon() + " " : "";
            categoryLabel.setText(icon + (product.getCategoryName() != null ? product.getCategoryName() : ""));
        }

        priceLabel.setText(String.format("%.0f руб.", product.getFinalPrice()));
        ratingLabel.setText(product.getStars() + String.format(" %.1f", product.getRating()));

        if (product.hasDiscount()) {
            oldPriceLabel.setText(String.format("%.0f руб.", product.getPrice()));
            oldPriceLabel.setVisible(true);
            discountLabel.setText("-" + product.getDiscountPercent() + "%");
            discountLabel.setVisible(true);
        } else {
            oldPriceLabel.setVisible(false);
            discountLabel.setVisible(false);
        }

        if (product.isInStock()) {
            stockLabel.setText("В наличии: " + product.getQuantityInStock() + " шт.");
            stockLabel.getStyleClass().remove("out-of-stock");
            addToCartBtn.setDisable(false);
        } else {
            stockLabel.setText("Нет в наличии");
            if (!stockLabel.getStyleClass().contains("out-of-stock"))
                stockLabel.getStyleClass().add("out-of-stock");
            addToCartBtn.setDisable(true);
        }

        boolean mgr = catalog != null && catalog.isManagerMode();
        addToCartBtn.setVisible(!mgr);
        addToCartBtn.setManaged(!mgr);

        if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            try {
                productImage.setImage(new Image(product.getImageUrl(), true));
            } catch (Exception ignored) {
            }
        }
    }

    @FXML
    public void handleAddToCart() {
        if (Session.getInstance().isGuest()) {
            AlertHelper.info("Требуется авторизация", "Войдите или зарегистрируйтесь для добавления в корзину.");
            return;
        }
        Session.getInstance().addToCart(product, 1);
        catalog.updateCartLabel();
        addToCartBtn.setText("Добавлено!");
        addToCartBtn.getStyleClass().add("btn-success");
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            javafx.application.Platform.runLater(() -> {
                addToCartBtn.setText("В корзину");
                addToCartBtn.getStyleClass().remove("btn-success");
            });
        }).start();
    }

    @FXML
    public void handleOpenDetail() {
        catalog.openProductDetail(product);
    }

    @FXML
    public void handleAddToCartConsumed(javafx.scene.input.MouseEvent e) {
        e.consume();
    }
}
