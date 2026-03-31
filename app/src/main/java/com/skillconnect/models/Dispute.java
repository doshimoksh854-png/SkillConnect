package com.skillconnect.models;

public class Dispute {
    private String documentId;
    private String bookingId;
    private String reporterId;       // Who filed the dispute
    private String reporterName;
    private String reporterRole;     // "customer" or "provider"
    private String againstId;        // Target user
    private String againstName;
    private String reason;           // Category: "quality", "payment", "behavior", "other"
    private String description;
    private String status;           // "open", "under_review", "resolved", "dismissed"
    private String resolution;       // Admin resolution note
    private long   timestamp;

    public Dispute() {}

    public Dispute(String bookingId, String reporterId, String reporterName,
                   String reporterRole, String againstId, String againstName,
                   String reason, String description) {
        this.bookingId    = bookingId;
        this.reporterId   = reporterId;
        this.reporterName = reporterName;
        this.reporterRole = reporterRole;
        this.againstId    = againstId;
        this.againstName  = againstName;
        this.reason       = reason;
        this.description  = description;
        this.status       = "open";
        this.timestamp    = System.currentTimeMillis();
    }

    public String getDocumentId()           { return documentId; }
    public void   setDocumentId(String v)   { documentId = v; }
    public String getBookingId()            { return bookingId; }
    public void   setBookingId(String v)    { bookingId = v; }
    public String getReporterId()           { return reporterId; }
    public void   setReporterId(String v)   { reporterId = v; }
    public String getReporterName()         { return reporterName; }
    public void   setReporterName(String v) { reporterName = v; }
    public String getReporterRole()         { return reporterRole; }
    public void   setReporterRole(String v) { reporterRole = v; }
    public String getAgainstId()            { return againstId; }
    public void   setAgainstId(String v)    { againstId = v; }
    public String getAgainstName()          { return againstName; }
    public void   setAgainstName(String v)  { againstName = v; }
    public String getReason()               { return reason; }
    public void   setReason(String v)       { reason = v; }
    public String getDescription()          { return description; }
    public void   setDescription(String v)  { description = v; }
    public String getStatus()               { return status; }
    public void   setStatus(String v)       { status = v; }
    public String getResolution()           { return resolution; }
    public void   setResolution(String v)   { resolution = v; }
    public long   getTimestamp()            { return timestamp; }
    public void   setTimestamp(long v)      { timestamp = v; }
}
