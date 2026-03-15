package model;

import java.time.LocalDateTime;

public class Product {
    private long id;
    private long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String name;
    private String description;
    private double price;
    private int quantityInStock;
    private String imageUrl;
    private double rating;
    private int reviewsCount;
    private int discountPercent;
    private boolean available;
    private String brand;
    private LocalDateTime createdAt;

    public Product() {
    }

    public double getFinalPrice() {
        if (discountPercent > 0) {
            return price * (1.0 - discountPercent / 100.0);
        }
        return price;
    }

    public boolean hasDiscount() {
        return discountPercent > 0;
    }

    public boolean isInStock() {
        return quantityInStock > 0;
    }

    public String getStars() {
        int r = (int) Math.round(rating);
        return "★".repeat(r) + "☆".repeat(5 - r);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long v) {
        this.categoryId = v;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String v) {
        this.categoryName = v;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String v) {
        this.categoryIcon = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(int v) {
        this.quantityInStock = v;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String v) {
        this.imageUrl = v;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int v) {
        this.reviewsCount = v;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int v) {
        this.discountPercent = v;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean v) {
        this.available = v;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }
}
