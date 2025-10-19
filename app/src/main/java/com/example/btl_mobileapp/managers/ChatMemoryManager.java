package com.example.btl_mobileapp.managers;

import com.example.btl_mobileapp.models.Message;
import com.example.btl_mobileapp.models.MessageChatBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatMemoryManager {

    private static ChatMemoryManager instance;
    private final Map<String, List<MessageChatBot>> chatMemory;

    private ChatMemoryManager() {
        chatMemory = new HashMap<>();
    }

    public static synchronized ChatMemoryManager getInstance() {
        if (instance == null) {
            instance = new ChatMemoryManager();
        }
        return instance;
    }

    public void addMessage(String chatId, MessageChatBot message) {
        List<MessageChatBot> messages = chatMemory.getOrDefault(chatId, new ArrayList<>());
        messages.add(message);
        chatMemory.put(chatId, messages);
    }

    public List<MessageChatBot> getMessages(String chatId) {
        return chatMemory.getOrDefault(chatId, new ArrayList<>());
    }

    public void clearChat(String chatId) {
        chatMemory.remove(chatId);
    }

    public void clearAll() {
        chatMemory.clear();
    }
}
