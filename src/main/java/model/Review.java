package model;

import java.time.LocalDateTime;

public class Review {
    private long id;
    private long productId;
    private String productName;
    private long userId;
    private String username;
    private int rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;

    public Review() {
    }

    public Review(long userId, long productId, int rating, String title, String comment) {
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
    }

    public String getStars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long v) {
        this.productId = v;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String v) {
        this.productName = v;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long v) {
        this.userId = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }
}
