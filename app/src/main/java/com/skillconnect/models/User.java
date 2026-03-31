package com.skillconnect.models;

/**
 * User model — V4: added profileImageUrl, referralCode, trustScore, badgeLevel
 */
public class User {
    private String id;   // Firebase Auth UID
    private String name;
    private String email;
    private String role; // "customer" or "provider"
    private String phone;
    private String profileImageUrl;
    private String referralCode;
    private String referredBy;
    private double trustScore;      // 0-100 composite score
    private String badgeLevel;      // "bronze", "silver", "gold", "expert"
    private int    completedJobs;
    private int    totalJobs;
    private long createdAt;

    public User() {}

    public User(String name, String email, String role) {
        this.name  = name;
        this.email = email;
        this.role  = role;
    }

    public String getId()    { return id; }
    public void setId(String id) { this.id = id; }

    public String getName()  { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole()  { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String url) { this.profileImageUrl = url; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String code) { this.referralCode = code; }

    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }

    public double getTrustScore() { return trustScore; }
    public void setTrustScore(double score) { this.trustScore = score; }

    public String getBadgeLevel() { return badgeLevel; }
    public void setBadgeLevel(String level) { this.badgeLevel = level; }

    public int getCompletedJobs() { return completedJobs; }
    public void setCompletedJobs(int n) { this.completedJobs = n; }

    public int getTotalJobs() { return totalJobs; }
    public void setTotalJobs(int n) { this.totalJobs = n; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
