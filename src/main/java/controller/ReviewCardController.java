package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import model.Review;
import service.ReviewService;
import util.Session;

public class ReviewCardController {

    @FXML
    private Label starsLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label commentLabel;
    @FXML
    private Button deleteBtn;

    private Review review;
    private Runnable onDelete;
    private final ReviewService reviewSvc = new ReviewService();

    public void setReview(Review review, Runnable onDelete) {
        this.review = review;
        this.onDelete = onDelete;

        starsLabel.setText(review.getStars());
        authorLabel.setText(review.getUsername() != null ? review.getUsername() : "Пользователь");
        titleLabel.setText(review.getTitle() != null ? review.getTitle() : "");
        commentLabel.setText(review.getComment() != null ? review.getComment() : "");

        boolean own = Session.getInstance().isLoggedIn()
                && Session.getInstance().getCurrentUser().getId() == review.getUserId();
        deleteBtn.setVisible(own);
        deleteBtn.setManaged(own);
    }

    @FXML
    public void handleDelete() {
        reviewSvc.delete(review.getId());
        if (onDelete != null) onDelete.run();
    }
}
