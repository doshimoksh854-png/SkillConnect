package com.skillconnect.models;

public class Notification {
    private String id;
    private String userId;
    private String role;      // "customer" or "provider"
    private String type;      // e.g. "bid_received", "booking_confirmed", "payment_received"
    private String title;
    private String message;
    private String relatedId; // jobId, bookingId, etc.
    private boolean isRead;
    private long createdAt;
    private String actionRoute;

    public Notification() {}

    public Notification(String userId, String role, String type,
                        String title, String message, String relatedId) {
        this.userId     = userId;
        this.role       = role;
        this.type       = type;
        this.title      = title;
        this.message    = message;
        this.relatedId  = relatedId;
        this.isRead     = false;
        this.createdAt  = System.currentTimeMillis();
    }

    public String getId()               { return id; }
    public void   setId(String v)       { id = v; }

    public String getUserId()           { return userId; }
    public void   setUserId(String v)   { userId = v; }

    public String getRole()             { return role; }
    public void   setRole(String v)     { role = v; }

    public String getType()             { return type; }
    public void   setType(String v)     { type = v; }

    public String getTitle()            { return title; }
    public void   setTitle(String v)    { title = v; }

    public String getMessage()          { return message; }
    public void   setMessage(String v)  { message = v; }

    public String getRelatedId()        { return relatedId; }
    public void   setRelatedId(String v){ relatedId = v; }

    public boolean isRead()             { return isRead; }
    public void    setRead(boolean v)   { isRead = v; }

    public long  getCreatedAt()         { return createdAt; }
    public void  setCreatedAt(long v)   { createdAt = v; }

    public String getActionRoute()             { return actionRoute; }
    public void   setActionRoute(String v)     { actionRoute = v; }
}
