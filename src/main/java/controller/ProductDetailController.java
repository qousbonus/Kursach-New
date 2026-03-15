package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import model.Product;
import model.Review;
import service.ReviewService;
import util.AlertHelper;
import util.SceneManager;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProductDetailController implements Initializable {

    @FXML
    private ImageView productImage;
    @FXML
    private Label nameLabel;
    @FXML
    private Label brandLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label oldPriceLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label stockLabel;
    @FXML
    private Label ratingLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Spinner<Integer> quantitySpinner;
    @FXML
    private Button addToCartBtn;
    @FXML
    private VBox reviewsBox;
    @FXML
    private Label emptyReviewsLabel;
    @FXML
    private VBox reviewFormBox;
    @FXML
    private Button star1, star2, star3, star4, star5;
    @FXML
    private Label ratingValueLabel;
    @FXML
    private TextField reviewTitleField;
    @FXML
    private TextArea reviewCommentArea;
    @FXML
    private Label reviewErrorLabel;

    private static final String[] RATING_LABELS = {"", "Плохо", "Неплохо", "Хорошо", "Отлично", "Превосходно!"};

    private Product product;
    private CatalogController catalog;
    private int selectedRating = 0;
    private final ReviewService reviewSvc = new ReviewService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
    }

    public void setProduct(Product product, CatalogController catalog) {
        this.product = product;
        this.catalog = catalog;
        bind();
        loadReviews();
    }

    private void bind() {
        nameLabel.setText(product.getName());
        brandLabel.setText(product.getBrand() != null ? product.getBrand() : "");
        descriptionArea.setText(product.getDescription() != null ? product.getDescription() : "");
        descriptionArea.setEditable(false);
        priceLabel.setText(String.format("%.0f руб.", product.getFinalPrice()));
        ratingLabel.setText(product.getStars() + String.format("  %.1f / 5.0  (%d отзывов)", product.getRating(), product.getReviewsCount()));

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
            addToCartBtn.setDisable(false);
        } else {
            stockLabel.setText("Нет в наличии");
            addToCartBtn.setDisable(true);
        }

        boolean mgr = catalog != null && catalog.isManagerMode();
        addToCartBtn.setVisible(!mgr);
        addToCartBtn.setManaged(!mgr);
        quantitySpinner.setVisible(!mgr);
        quantitySpinner.setManaged(!mgr);

        if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            try {
                productImage.setImage(new Image(product.getImageUrl(), true));
            } catch (Exception ignored) {
            }
        }

        if (reviewFormBox != null) {
            boolean show = Session.getInstance().isLoggedIn();
            reviewFormBox.setVisible(show);
            reviewFormBox.setManaged(show);
        }
    }

    private void loadReviews() {
        reviewsBox.getChildren().clear();
        List<Review> reviews = reviewSvc.getByProduct(product.getId());

        int count = reviews.size();
        double avg = count > 0
                ? Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10.0) / 10.0
                : 0.0;
        String stars = count == 0 ? "☆☆☆☆☆" : starsFor(avg);
        ratingLabel.setText(stars + String.format("  %.1f / 5.0  (%d отзывов)", avg, count));

        emptyReviewsLabel.setVisible(reviews.isEmpty());
        emptyReviewsLabel.setManaged(reviews.isEmpty());

        long myId = Session.getInstance().isLoggedIn() ? Session.getInstance().getCurrentUser().getId() : -1;
        boolean alreadyWrote = false;

        for (Review r : reviews) {
            try {
                FXMLLoader loader = util.FxmlUtil.loader("/fxml/review_card.fxml");
                Parent card = loader.load();
                ReviewCardController ctrl = loader.getController();
                ctrl.setReview(r, this::loadReviews);
                reviewsBox.getChildren().add(card);
                if (r.getUserId() == myId) alreadyWrote = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (reviewFormBox != null) {
            boolean canWrite = Session.getInstance().isLoggedIn() && !alreadyWrote;
            reviewFormBox.setVisible(canWrite);
            reviewFormBox.setManaged(canWrite);
        }
    }

    @FXML
    public void handleStar1() {
        setRating(1);
    }

    @FXML
    public void handleStar2() {
        setRating(2);
    }

    @FXML
    public void handleStar3() {
        setRating(3);
    }

    @FXML
    public void handleStar4() {
        setRating(4);
    }

    @FXML
    public void handleStar5() {
        setRating(5);
    }

    private void setRating(int rating) {
        selectedRating = rating;
        ratingValueLabel.setText(RATING_LABELS[rating]);
        Button[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++) {
            stars[i].getStyleClass().remove("active");
            if (i < rating) stars[i].getStyleClass().add("active");
        }
    }

    @FXML
    public void handleAddToCart() {
        if (Session.getInstance().isGuest()) {
            AlertHelper.info("Требуется авторизация", "Войдите или зарегистрируйтесь для добавления в корзину.");
            return;
        }
        int qty = quantitySpinner.getValue();
        Session.getInstance().addToCart(product, qty);
        if (catalog != null) catalog.updateCartLabel();
        AlertHelper.info("Корзина", "Добавлено " + qty + " шт. «" + product.getName() + "»");
    }

    @FXML
    public void handleSubmitReview() {
        if (!Session.getInstance().isLoggedIn()) {
            reviewErrorLabel.setText("Войдите в аккаунт");
            return;
        }
        if (selectedRating == 0) {
            reviewErrorLabel.setText("Выберите оценку (нажмите на звезду)");
            return;
        }
        String comment = reviewCommentArea.getText().trim();
        if (comment.isEmpty()) {
            reviewErrorLabel.setText("Напишите комментарий");
            return;
        }

        String err = reviewSvc.submit(product.getId(), selectedRating, reviewTitleField.getText().trim(), comment);
        if (err != null) {
            reviewErrorLabel.setText(err);
        } else {
            reviewErrorLabel.setText("");
            reviewCommentArea.clear();
            reviewTitleField.clear();
            setRating(0);
            AlertHelper.info("Спасибо!", "Ваш отзыв успешно опубликован!");
            loadReviews();
        }
    }

    @FXML
    public void handleClose() {
        SceneManager.showCatalog();
    }

    private String starsFor(double rating) {
        int full = Math.max(0, Math.min(5, (int) Math.round(rating)));
        return "★".repeat(full) + "☆".repeat(5 - full);
    }
}
