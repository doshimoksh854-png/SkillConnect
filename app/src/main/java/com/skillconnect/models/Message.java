package com.skillconnect.models;

public class Message {
    private String id;
    private String chatId;
    private String senderId;
    private String senderName;
    private String text;
    private String imageUrl;
    private long   timestamp;

    public Message() {} // required by Firestore

    public Message(String chatId, String senderId, String senderName, String text) {
        this.chatId     = chatId;
        this.senderId   = senderId;
        this.senderName = senderName;
        this.text       = text;
        this.timestamp  = System.currentTimeMillis();
    }

    public String getId()         { return id; }
    public void   setId(String v) { id = v; }

    public String getChatId()         { return chatId; }
    public void   setChatId(String v) { chatId = v; }

    public String getSenderId()         { return senderId; }
    public void   setSenderId(String v) { senderId = v; }

    public String getSenderName()         { return senderName; }
    public void   setSenderName(String v) { senderName = v; }

    public String getText()         { return text; }
    public void   setText(String v) { text = v; }

    public String getImageUrl()         { return imageUrl; }
    public void   setImageUrl(String v) { imageUrl = v; }

    public long  getTimestamp()      { return timestamp; }
    public void  setTimestamp(long v){ timestamp = v; }
}
