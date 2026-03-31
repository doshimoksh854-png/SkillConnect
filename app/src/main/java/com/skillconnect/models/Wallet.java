package com.skillconnect.models;

public class Wallet {
    private String userId;
    private double balance;
    private double pendingBalance;
    private String currency;
    private long   updatedAt;

    public Wallet() {}

    public Wallet(String userId) {
        this.userId         = userId;
        this.balance        = 0.0;
        this.pendingBalance = 0.0;
        this.currency       = "INR";
        this.updatedAt      = System.currentTimeMillis();
    }

    public String getUserId()               { return userId; }
    public void   setUserId(String v)       { userId = v; }

    public double getBalance()              { return balance; }
    public void   setBalance(double v)      { balance = v; }

    public double getPendingBalance()           { return pendingBalance; }
    public void   setPendingBalance(double v)   { pendingBalance = v; }

    public String getCurrency()             { return currency; }
    public void   setCurrency(String v)     { currency = v; }

    public long  getUpdatedAt()             { return updatedAt; }
    public void  setUpdatedAt(long v)       { updatedAt = v; }
}
