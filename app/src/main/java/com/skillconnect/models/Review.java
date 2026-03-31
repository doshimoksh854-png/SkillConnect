package com.skillconnect.models;

/**
 * Review model — V3: documentId for Firestore, String userId/skillId/bookingId
 */
public class Review {
    private String documentId;
    private String userId;
    private String skillId;
    private String bookingId;
    private float rating;
    private String comment;
    private String userName;
    private long createdAt;

    public Review() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String d) { this.documentId = d; }

    public int getId() { return documentId != null ? documentId.hashCode() : 0; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
