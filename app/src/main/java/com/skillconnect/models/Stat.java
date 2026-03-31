package com.skillconnect.models;

/**
 * Model class for dashboard statistics
 */
public class Stat {
    private String label;
    private String value;
    private int iconResource;

    public Stat(String label, String value, int iconResource) {
        this.label = label;
        this.value = value;
        this.iconResource = iconResource;
    }

    // Getters
    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public int getIconResource() {
        return iconResource;
    }

    // Setters
    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }
}
