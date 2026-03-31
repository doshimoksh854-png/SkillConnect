package com.skillconnect.models;

public class ChatThread {
    private String id; // usually uid1_uid2
    private String partnerId;
    private String partnerName;
    private String lastMessage;
    private long lastTimestamp;
    private int unreadCount;

    public ChatThread() {}

    public ChatThread(String partnerId, String partnerName, String lastMessage, long lastTimestamp, int unreadCount) {
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.unreadCount = unreadCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    
    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
