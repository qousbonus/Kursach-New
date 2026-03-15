package model;

public class Category {
    private long id;
    private String name;
    private String description;
    private String icon;

    public Category() {
    }

    public Category(long id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public String getDisplayName() {
        return (icon != null && !icon.isBlank() ? icon + " " : "") + name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String i) {
        this.icon = i;
    }

    @Override
    public String toString() {
        return name;
    }
}
