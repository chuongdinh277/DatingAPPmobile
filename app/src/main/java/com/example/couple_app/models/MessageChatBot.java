package com.example.couple_app.models;

public class MessageChatBot {
    private String senderId;
    private String content;
    private long timestamp;

    public MessageChatBot(String senderId, String content, long timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isFromAI() {
        return "ai_assistant".equals(senderId);
    }

    public void setContent(String content) {
        this.content = content;
    }
}
