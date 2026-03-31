package com.skillconnect.models;

public class Transaction {
    // type: "topup" | "payment" | "received" | "refund" | "withdrawal"
    // status: "pending" | "success" | "failed" | "held" | "released" | "refunded"
    // method: "wallet" | "upi" | "card" | "cash"

    private String transactionId;
    private String userId;
    private String bookingId;
    private String type;
    private double amount;
    private String status;
    private String method;
    private String description;
    private long   createdAt;

    public Transaction() {}

    public Transaction(String userId, String bookingId, String type,
                       double amount, String status, String method, String description) {
        this.userId      = userId;
        this.bookingId   = bookingId;
        this.type        = type;
        this.amount      = amount;
        this.status      = status;
        this.method      = method;
        this.description = description;
        this.createdAt   = System.currentTimeMillis();
    }

    public String getTransactionId()            { return transactionId; }
    public void   setTransactionId(String v)    { transactionId = v; }

    public String getUserId()                   { return userId; }
    public void   setUserId(String v)           { userId = v; }

    public String getBookingId()                { return bookingId; }
    public void   setBookingId(String v)        { bookingId = v; }

    public String getType()                     { return type; }
    public void   setType(String v)             { type = v; }

    public double getAmount()                   { return amount; }
    public void   setAmount(double v)           { amount = v; }

    public String getStatus()                   { return status; }
    public void   setStatus(String v)           { status = v; }

    public String getMethod()                   { return method; }
    public void   setMethod(String v)           { method = v; }

    public String getDescription()              { return description; }
    public void   setDescription(String v)      { description = v; }

    public long  getCreatedAt()                 { return createdAt; }
    public void  setCreatedAt(long v)           { createdAt = v; }
}
