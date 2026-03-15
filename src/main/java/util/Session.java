package util;

import model.CartItem;
import model.Product;
import model.User;
import repository.CartRepository;

import java.util.ArrayList;
import java.util.List;

public class Session {

    private static Session instance;
    private User currentUser;
    private final List<CartItem> cart = new ArrayList<>();
    private final CartRepository cartRepo = new CartRepository();

    private Session() {
    }

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void loadCartFromDb() {
        if (currentUser == null) {
            return;
        }
        cart.clear();
        cart.addAll(cartRepo.findByUser(currentUser.getId()));
    }

    public void logout() {
        this.currentUser = null;
        cart.clear();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isGuest() {
        return currentUser == null;
    }

    public boolean isAdmin() {
        return isLoggedIn() && currentUser.isAdmin();
    }

    public boolean isManager() {
        return isLoggedIn() && currentUser.isManager();
    }

    public boolean isUser() {
        return isLoggedIn() && currentUser.isUser();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void addToCart(Product product, int qty) {
        for (CartItem item : cart) {
            if (item.getProduct().getId() == product.getId()) {
                int newQty = item.getQuantity() + qty;
                item.setQuantity(newQty);
                if (isLoggedIn()) {
                    cartRepo.upsert(currentUser.getId(), product.getId(), newQty);
                }
                return;
            }
        }
        cart.add(new CartItem(product, qty));
        if (isLoggedIn()) {
            cartRepo.upsert(currentUser.getId(), product.getId(), qty);
        }
    }

    public void removeFromCart(long productId) {
        cart.removeIf(i -> i.getProduct().getId() == productId);
        if (isLoggedIn()) {
            cartRepo.delete(currentUser.getId(), productId);
        }
    }

    public void updateCartQty(long productId, int qty) {
        if (qty <= 0) {
            removeFromCart(productId);
            return;
        }
        for (CartItem item : cart) {
            if (item.getProduct().getId() == productId) {
                item.setQuantity(qty);
                if (isLoggedIn()) {
                    cartRepo.upsert(currentUser.getId(), productId, qty);
                }
                return;
            }
        }
    }

    public void clearCart() {
        cart.clear();
        if (isLoggedIn()) {
            cartRepo.deleteAll(currentUser.getId());
        }
    }

    public List<CartItem> getCart() {
        return cart;
    }

    public int getCartCount() {
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getCartTotal() {
        return cart.stream().mapToDouble(CartItem::getSubtotal).sum();
    }
}
