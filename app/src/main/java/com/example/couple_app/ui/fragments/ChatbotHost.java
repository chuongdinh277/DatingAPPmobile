package com.example.couple_app.ui.fragments;

import androidx.annotation.NonNull;

/**
 * Host callback interface to receive chatbot bottom sheet lifecycle events.
 */
public interface ChatbotHost {
    void onChatbotDismiss(@NonNull String sessionId);
}

