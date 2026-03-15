package model;

import java.time.LocalDateTime;

public class PromoCode {
    private long id;
    private String code;
    private int discountPercent;
    private int maxUses;
    private int usedCount;
    private LocalDateTime expiresAt;
    private boolean active;

    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (usedCount >= maxUses) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int v) {
        this.discountPercent = v;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int v) {
        this.maxUses = v;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int v) {
        this.usedCount = v;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime v) {
        this.expiresAt = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }
}
