package com.skillconnect.models;

/**
 * Model class for service categories
 */
public class Category {
    private int id;
    private String name;
    private String icon; // Icon resource name or emoji
    private int iconResource; // Drawable resource ID

    public Category(int id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public Category(int id, String name, int iconResource) {
        this.id = id;
        this.name = name;
        this.iconResource = iconResource;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getIconResource() {
        return iconResource;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }
}
