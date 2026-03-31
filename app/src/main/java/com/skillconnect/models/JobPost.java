package com.skillconnect.models;

public class JobPost {
    private String documentId;
    private String customerId;
    private String customerName;
    private String title;
    private String description;
    private String category;
    private double budget;
    private String status; // "open", "awarded", "closed"
    private long   timestamp;
    // New fields — image/file attachment + deadline
    private String imageUrl;
    private String attachmentUrl;
    private String attachmentName;
    private long   deadline; // epoch ms; 0 = not set

    public JobPost() {}

    public JobPost(String customerId, String customerName, String title,
                   String description, String category, double budget) {
        this.customerId   = customerId;
        this.customerName = customerName;
        this.title        = title;
        this.description  = description;
        this.category     = category;
        this.budget       = budget;
        this.status       = "open";
        this.timestamp    = System.currentTimeMillis();
    }

    public String getDocumentId()         { return documentId; }
    public void   setDocumentId(String v) { documentId = v; }

    public String getCustomerId()         { return customerId; }
    public void   setCustomerId(String v) { customerId = v; }

    public String getCustomerName()         { return customerName; }
    public void   setCustomerName(String v) { customerName = v; }

    public String getTitle()         { return title; }
    public void   setTitle(String v) { title = v; }

    public String getDescription()         { return description; }
    public void   setDescription(String v) { description = v; }

    public String getCategory()         { return category; }
    public void   setCategory(String v) { category = v; }

    public double getBudget()         { return budget; }
    public void   setBudget(double v) { budget = v; }

    public String getStatus()         { return status; }
    public void   setStatus(String v) { status = v; }

    public long   getTimestamp()      { return timestamp; }
    public void   setTimestamp(long v){ timestamp = v; }

    // ── Attachment & Deadline getters/setters ─────────────────────────────

    public String getImageUrl()          { return imageUrl; }
    public void   setImageUrl(String v)  { imageUrl = v; }

    public String getAttachmentUrl()         { return attachmentUrl; }
    public void   setAttachmentUrl(String v) { attachmentUrl = v; }

    public String getAttachmentName()         { return attachmentName; }
    public void   setAttachmentName(String v) { attachmentName = v; }

    public long   getDeadline()       { return deadline; }
    public void   setDeadline(long v) { deadline = v; }
}
