package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import model.CartItem;
import util.Session;

public class CartItemController {

    @FXML
    private Label nameLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Spinner<Integer> quantitySpinner;

    private CartItem cartItem;
    private Runnable onChanged;
    private Runnable onRemoved;

    public void setCartItem(CartItem item, Runnable onChanged) {
        setCartItem(item, onChanged, null);
    }

    public void setCartItem(CartItem item, Runnable onChanged, Runnable onRemoved) {
        this.cartItem = item;
        this.onChanged = onChanged;
        this.onRemoved = onRemoved;

        nameLabel.setText(item.getProduct().getName());
        updatePriceLabel(item.getQuantity());

        quantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, item.getQuantity()));

        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            Session.getInstance().updateCartQty(item.getProduct().getId(), newVal);
            updatePriceLabel(newVal);
            if (onChanged != null) onChanged.run();
        });
    }

    private void updatePriceLabel(int qty) {
        double price = cartItem.getProduct().getFinalPrice();
        priceLabel.setText(String.format("%.0f руб. x %d = %.0f руб.", price, qty, price * qty));
    }

    @FXML
    public void handleRemove() {
        Session.getInstance().removeFromCart(cartItem.getProduct().getId());
        if (onRemoved != null) onRemoved.run();
        else if (onChanged != null) onChanged.run();
    }
}
