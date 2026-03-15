package controller.manager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.PromoCode;
import service.PromoService;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class PromoFormController implements Initializable {

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField codeField;
    @FXML
    private TextField discountField;
    @FXML
    private TextField maxUsesField;
    @FXML
    private TextField expiresField;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private Label errorLabel;

    private final PromoService promoSvc = new PromoService();
    private PromoCode editPromo;
    private Runnable onSaved;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void setPromo(PromoCode promo, Runnable onSaved) {
        this.onSaved = onSaved;
        this.editPromo = promo;
        if (promo == null) return;

        formTitleLabel.setText("Редактировать промокод");
        codeField.setText(promo.getCode());
        discountField.setText(String.valueOf(promo.getDiscountPercent()));
        maxUsesField.setText(String.valueOf(promo.getMaxUses()));
        activeCheck.setSelected(promo.isActive());
        if (promo.getExpiresAt() != null)
            expiresField.setText(promo.getExpiresAt().toLocalDate().toString());
    }

    @FXML
    public void handleSave() {
        errorLabel.setText("");
        String code = codeField.getText().trim();
        String discStr = discountField.getText().trim();
        String maxStr = maxUsesField.getText().trim();
        String expStr = expiresField.getText().trim();

        int discount, maxUses;
        try {
            discount = Integer.parseInt(discStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Некорректный процент скидки");
            return;
        }
        try {
            maxUses = Integer.parseInt(maxStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Некорректное количество использований");
            return;
        }

        LocalDateTime expires = null;
        if (!expStr.isEmpty()) {
            try {
                expires = LocalDateTime.parse(expStr + "T23:59:59");
            } catch (Exception e) {
                errorLabel.setText("Формат даты: ГГГГ-ММ-ДД (например 2025-12-31)");
                return;
            }
        }

        PromoCode pc = editPromo != null ? editPromo : new PromoCode();
        pc.setCode(code);
        pc.setDiscountPercent(discount);
        pc.setMaxUses(maxUses);
        pc.setActive(activeCheck.isSelected());
        pc.setExpiresAt(expires);

        String err = promoSvc.save(pc);
        if (err != null) {
            errorLabel.setText(err);
        } else {
            if (onSaved != null) onSaved.run();
            ((Stage) codeField.getScene().getWindow()).close();
        }
    }

    @FXML
    public void handleCancel() {
        ((Stage) codeField.getScene().getWindow()).close();
    }
}