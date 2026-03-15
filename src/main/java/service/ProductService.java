package service;

import model.Category;
import model.Product;
import repository.CategoryRepository;
import repository.LogRepository;
import repository.ProductRepository;
import util.Session;

import java.util.List;

public class ProductService {

    private final ProductRepository productRepo = new ProductRepository();
    private final CategoryRepository catRepo = new CategoryRepository();
    private final LogRepository logRepo = new LogRepository();

    public List<Product> getAvailable() {
        try {
            return productRepo.findAvailable();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Product> getAll() {
        try {
            return productRepo.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Product> getByCategory(long catId) {
        try {
            return productRepo.findByCategory(catId);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Product> search(String q) {
        try {
            return productRepo.search(q);
        } catch (Exception e) {
            return List.of();
        }
    }

    public Product getById(long id) {
        try {
            return productRepo.findById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Category> getCategories() {
        try {
            return catRepo.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public String save(Product p) {
        if (p.getName() == null || p.getName().isBlank()) return "Введите название товара";
        if (p.getPrice() <= 0) return "Цена должна быть больше 0";
        if (p.getCategoryId() <= 0) return "Выберите категорию";
        try {
            if (p.getId() == 0) {
                productRepo.save(p);
                log("PRODUCT_CREATED", "Created: " + p.getName());
            } else {
                productRepo.update(p);
                log("PRODUCT_UPDATED", "Updated #" + p.getId());
            }
            return null;
        } catch (Exception e) {
            return "Ошибка сохранения: " + e.getMessage();
        }
    }

    public String updateStock(long productId, int newStock) {
        if (newStock < 0) return "Остаток не может быть отрицательным";
        try {
            productRepo.updateStock(productId, newStock);
            return null;
        } catch (Exception e) {
            return "Ошибка обновления остатка: " + e.getMessage();
        }
    }

    public String deactivate(long productId) {
        try {
            productRepo.deactivate(productId);
            return null;
        } catch (Exception e) {
            return "Ошибка деактивации: " + e.getMessage();
        }
    }

    public String hardDelete(long productId) {
        try {
            productRepo.hardDelete(productId);
            log("PRODUCT_DELETED", "Product #" + productId);
            return null;
        } catch (Exception e) {
            return "Ошибка удаления: " + e.getMessage();
        }
    }

    private void log(String action, String details) {
        Session s = Session.getInstance();
        if (s.isLoggedIn()) logRepo.log(s.getCurrentUser().getId(), action, "products", details);
    }
}
