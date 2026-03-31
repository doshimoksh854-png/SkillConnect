package com.skillconnect.models;

/**
 * Booking model — V3: documentId for Firestore, String userId/providerId
 */
public class Booking {
    private String documentId;  // Firestore auto-generated document ID
    private String userId;      // Firebase UID of customer
    private String providerId;  // Firebase UID of provider
    private String skillId;     // Firestore skill document ID
    private String skillTitle;
    private String providerName;
    private double price;
    private String status;      // pending, accepted, completed, cancelled, rejected
    private long bookingDate;
    private String notes;

    public Booking() {}

    public Booking(String userId, String providerId, String skillId,
                   String skillTitle, String providerName, double price) {
        this.userId      = userId;
        this.providerId  = providerId;
        this.skillId     = skillId;
        this.skillTitle  = skillTitle;
        this.providerName= providerName;
        this.price       = price;
        this.status      = "pending";
        this.bookingDate = System.currentTimeMillis();
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // Legacy int id support for adapters
    public int getId() { return documentId != null ? documentId.hashCode() : 0; }

    public String getUserId()   { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }

    public String getSkillTitle() { return skillTitle; }
    public void setSkillTitle(String skillTitle) { this.skillTitle = skillTitle; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public double getPrice()  { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getBookingDate() { return bookingDate; }
    public void setBookingDate(long d) { this.bookingDate = d; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
