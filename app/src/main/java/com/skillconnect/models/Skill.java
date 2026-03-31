package com.skillconnect.models;

/**
 * Skill model — V3: added documentId for Firestore document reference
 */
public class Skill {
    private String documentId; // Firestore auto-generated document ID
    private int id;            // kept for adapter compatibility (index-based)
    private String title;
    private String description;
    private double price;
    private String providerId;    // Firebase Auth UID of provider
    private String providerName;
    private float rating;
    private int reviewCount;
    private String categoryName;

    public Skill() {}

    public Skill(String title, String description, double price, String providerName, float rating) {
        this.title        = title;
        this.description  = description;
        this.price        = price;
        this.providerName = providerName;
        this.rating       = rating;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getFormattedPrice() { return "₹" + String.format("%.0f", price); }
}
