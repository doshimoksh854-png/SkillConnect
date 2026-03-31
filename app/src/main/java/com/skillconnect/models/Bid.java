package com.skillconnect.models;

public class Bid {
    private String documentId;
    private String jobId;
    private String providerId;
    private String providerName;
    private double bidAmount;
    private String proposal;
    private String status; // "pending", "accepted", "rejected"
    private long   timestamp;

    public Bid() {}

    public Bid(String jobId, String providerId, String providerName,
               double bidAmount, String proposal) {
        this.jobId        = jobId;
        this.providerId   = providerId;
        this.providerName = providerName;
        this.bidAmount    = bidAmount;
        this.proposal     = proposal;
        this.status       = "pending";
        this.timestamp    = System.currentTimeMillis();
    }

    public String getDocumentId()         { return documentId; }
    public void   setDocumentId(String v) { documentId = v; }

    public String getJobId()         { return jobId; }
    public void   setJobId(String v) { jobId = v; }

    public String getProviderId()         { return providerId; }
    public void   setProviderId(String v) { providerId = v; }

    public String getProviderName()         { return providerName; }
    public void   setProviderName(String v) { providerName = v; }

    public double getBidAmount()         { return bidAmount; }
    public void   setBidAmount(double v) { bidAmount = v; }

    public String getProposal()         { return proposal; }
    public void   setProposal(String v) { proposal = v; }

    public String getStatus()         { return status; }
    public void   setStatus(String v) { status = v; }

    public long   getTimestamp()      { return timestamp; }
    public void   setTimestamp(long v){ timestamp = v; }
}
