package com.skillconnect.models;

public class Payment {
    private String documentId;
    private String bookingId;
    private String customerId;
    private String providerId;
    private String customerName;
    private String providerName;
    private String jobTitle;
    private double amount;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String status; // "success", "failed", "refunded"
    private long   timestamp;

    public Payment() {}

    public Payment(String bookingId, String customerId, String providerId,
                   String customerName, String providerName, String jobTitle,
                   double amount) {
        this.bookingId    = bookingId;
        this.customerId   = customerId;
        this.providerId   = providerId;
        this.customerName = customerName;
        this.providerName = providerName;
        this.jobTitle     = jobTitle;
        this.amount       = amount;
        this.status       = "pending";
        this.timestamp    = System.currentTimeMillis();
    }

    public String getDocumentId()             { return documentId; }
    public void   setDocumentId(String v)     { documentId = v; }

    public String getBookingId()              { return bookingId; }
    public void   setBookingId(String v)      { bookingId = v; }

    public String getCustomerId()             { return customerId; }
    public void   setCustomerId(String v)     { customerId = v; }

    public String getProviderId()             { return providerId; }
    public void   setProviderId(String v)     { providerId = v; }

    public String getCustomerName()           { return customerName; }
    public void   setCustomerName(String v)   { customerName = v; }

    public String getProviderName()           { return providerName; }
    public void   setProviderName(String v)   { providerName = v; }

    public String getJobTitle()               { return jobTitle; }
    public void   setJobTitle(String v)       { jobTitle = v; }

    public double getAmount()                 { return amount; }
    public void   setAmount(double v)         { amount = v; }

    public String getRazorpayPaymentId()      { return razorpayPaymentId; }
    public void   setRazorpayPaymentId(String v) { razorpayPaymentId = v; }

    public String getRazorpayOrderId()        { return razorpayOrderId; }
    public void   setRazorpayOrderId(String v){ razorpayOrderId = v; }

    public String getStatus()                 { return status; }
    public void   setStatus(String v)         { status = v; }

    public long   getTimestamp()              { return timestamp; }
    public void   setTimestamp(long v)        { timestamp = v; }
}
