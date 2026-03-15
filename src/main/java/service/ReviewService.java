package service;

import model.Review;
import repository.LogRepository;
import repository.ReviewRepository;
import util.Session;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReviewService {

    private final ReviewRepository reviewRepo = new ReviewRepository();
    private final LogRepository logRepo = new LogRepository();

    public List<Review> getByProduct(long productId) {
        try {
            return reviewRepo.findByProduct(productId);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Review> getAll() {
        try {
            return reviewRepo.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public String submit(long productId, int rating, String title, String comment) {
        Session s = Session.getInstance();
        if (!s.isLoggedIn()) return "Необходима авторизация";
        if (rating < 1 || rating > 5) return "Выберите оценку (1-5 звёзд)";
        if (comment == null || comment.isBlank()) return "Напишите комментарий";

        try {
            reviewRepo.save(new Review(s.getCurrentUser().getId(), productId, rating, title, comment));
            reviewRepo.syncProductStats(productId);
            logRepo.log(s.getCurrentUser().getId(), "REVIEW_SUBMITTED", "reviews",
                    "Review for product #" + productId + " rating=" + rating);
            return null;
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("23505") || msg.contains("duplicate key") || msg.contains("reviews_product_id_user_id_key"))
                return "Вы уже оставляли отзыв на этот товар";
            return "Ошибка отправки: " + msg;
        }
    }

    public void syncAllProductStats() {
        try {
            Map<Long, List<Review>> byProduct = reviewRepo.findAll().stream()
                    .collect(Collectors.groupingBy(Review::getProductId));
            byProduct.keySet().forEach(id -> {
                try {
                    reviewRepo.syncProductStats(id);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public void delete(long reviewId) {
        try {
            reviewRepo.reject(reviewId);
        } catch (Exception ignored) {
        }
    }

    public void reject(long reviewId) {
        try {
            long productId = reviewRepo.findAll().stream()
                    .filter(r -> r.getId() == reviewId)
                    .mapToLong(Review::getProductId)
                    .findFirst().orElse(-1);
            reviewRepo.reject(reviewId);
            if (productId > 0) reviewRepo.syncProductStats(productId);
        } catch (Exception ignored) {
        }
    }
}
