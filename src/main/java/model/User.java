package model;

import java.time.LocalDateTime;

public class User {
    private long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private boolean active;
    private boolean accountLocked;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public User() {
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isManager() {
        return "manager".equals(role);
    }

    public boolean isUser() {
        return "user".equals(role);
    }

    public String getDisplayName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? username : full;
    }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? username : full;
    }

    public String getRoleDisplay() {
        return switch (role == null ? "" : role) {
            case "admin" -> "Администратор";
            case "manager" -> "Менеджер";
            default -> "Покупатель";
        };
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String u) {
        this.username = u;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String f) {
        this.firstName = f;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String l) {
        this.lastName = l;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean v) {
        this.accountLocked = v;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime v) {
        this.lastLogin = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }
}
