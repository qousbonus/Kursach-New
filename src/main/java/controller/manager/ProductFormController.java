package controller.manager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Category;
import model.Product;
import service.ProductService;
import util.AlertHelper;
import util.SupabaseStorage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ProductFormController implements Initializable {

    @FXML
    private Label formTitleLabel;
    @FXML
    private ImageView previewImage;
    @FXML
    private Label imageStatusLabel;
    @FXML
    private Button clearImageBtn;
    @FXML
    private TextField nameField;
    @FXML
    private TextField brandField;
    @FXML
    private ComboBox<Category> categoryCombo;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField discountField;
    @FXML
    private TextField stockField;
    @FXML
    private CheckBox availableCheck;
    @FXML
    private Label errorLabel;

    private final ProductService prodSvc = new ProductService();
    private Product editProduct;
    private Runnable onSaved;
    private File selectedImageFile;
    private String currentImageUrl;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryCombo.getItems().setAll(prodSvc.getCategories());
    }

    public void setProduct(Product product, Runnable onSaved) {
        this.onSaved = onSaved;
        this.editProduct = product;
        if (product == null) return;

        formTitleLabel.setText("Редактировать товар");
        nameField.setText(product.getName());
        brandField.setText(product.getBrand() != null ? product.getBrand() : "");
        descriptionField.setText(product.getDescription() != null ? product.getDescription() : "");
        priceField.setText(String.valueOf((int) product.getPrice()));
        discountField.setText(String.valueOf(product.getDiscountPercent()));
        stockField.setText(String.valueOf(product.getQuantityInStock()));
        availableCheck.setSelected(product.isAvailable());
        currentImageUrl = product.getImageUrl();

        if (currentImageUrl != null && !currentImageUrl.isBlank()) {
            try {
                previewImage.setImage(new Image(currentImageUrl, true));
            } catch (Exception ignored) {

            }
            imageStatusLabel.setText("Текущее фото загружено");
            if (clearImageBtn != null) clearImageBtn.setVisible(true);
        } else {
            if (clearImageBtn != null) clearImageBtn.setVisible(false);
        }

        categoryCombo.getItems().stream()
                .filter(c -> c.getId() == product.getCategoryId())
                .findFirst()
                .ifPresent(categoryCombo::setValue);
    }

    @FXML
    public void handleClearImage() {
        currentImageUrl = null;
        selectedImageFile = null;
        previewImage.setImage(null);
        imageStatusLabel.setText("Фото удалено — товар будет без изображения");
        if (clearImageBtn != null) clearImageBtn.setVisible(false);
    }

    @FXML
    public void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Выбрать фото товара");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(previewImage.getScene().getWindow());
        if (file == null) return;
        selectedImageFile = file;
        imageStatusLabel.setText("Выбрано: " + file.getName());
        try {
            previewImage.setImage(new Image(file.toURI().toString(), true));
        } catch (Exception ignored) {
        }
        if (clearImageBtn != null) clearImageBtn.setVisible(true);
    }

    @FXML
    public void handleSave() {
        errorLabel.setText("");
        String name = nameField.getText().trim();
        String brand = brandField.getText().trim();
        String desc = descriptionField.getText().trim();
        Category cat = categoryCombo.getValue();
        String priceStr = priceField.getText().trim();
        String discStr = discountField.getText().trim();
        String stockStr = stockField.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Введите название");
            return;
        }
        if (cat == null) {
            errorLabel.setText("Выберите категорию");
            return;
        }
        if (priceStr.isEmpty()) {
            errorLabel.setText("Введите цену");
            return;
        }

        double price;
        int discount = 0, stock = 0;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Некорректная цена");
            return;
        }
        try {
            if (!discStr.isEmpty()) discount = Integer.parseInt(discStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Некорректная скидка");
            return;
        }
        try {
            if (!stockStr.isEmpty()) stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Некорректный остаток");
            return;
        }

        String imageUrl = currentImageUrl;
        if (selectedImageFile != null) {
            imageStatusLabel.setText("Загрузка фото...");
            SupabaseStorage.UploadResult res = new SupabaseStorage().upload(selectedImageFile);
            if (!res.success) {
                errorLabel.setText("Ошибка загрузки фото: " + res.error);
                return;
            }
            imageUrl = res.publicUrl;
        }

        Product p = editProduct != null ? editProduct : new Product();
        p.setName(name);
        p.setBrand(brand.isEmpty() ? null : brand);
        p.setDescription(desc.isEmpty() ? null : desc);
        p.setCategoryId(cat.getId());
        p.setPrice(price);
        p.setDiscountPercent(discount);
        p.setQuantityInStock(stock);
        p.setAvailable(availableCheck.isSelected());
        p.setImageUrl(imageUrl);

        String err = prodSvc.save(p);
        if (err != null) {
            errorLabel.setText(err);
        } else {
            if (onSaved != null) onSaved.run();
            ((Stage) nameField.getScene().getWindow()).close();
        }
    }

    @FXML
    public void handleCancel() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}
