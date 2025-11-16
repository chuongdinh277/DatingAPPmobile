package com.example.couple_app.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.couple_app.api.ChatBotApiService;
import com.example.couple_app.api.RetrofitClient;
import com.example.couple_app.models.ChatRequest;
import com.example.couple_app.models.ChatResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for chatbot API calls
 * Separates network logic from UI layer
 */
public class ChatBotRepository {
    private static final String TAG = "ChatBotRepository";

    private final ChatBotApiService apiService;

    public ChatBotRepository() {
        this.apiService = RetrofitClient.getInstance().getApiService();
    }

    /**
     * Send message to chatbot backend
     * @param message User's message
     * @param sessionId Unique session identifier
     * @param callback Callback for response
     */
    public void sendMessage(@NonNull String message, @NonNull String sessionId,
                           @NonNull ChatBotCallback callback) {
        if (message.trim().isEmpty()) {
            callback.onError("Message cannot be empty");
            return;
        }

        ChatRequest request = new ChatRequest(message, sessionId);

        apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call,
                                 @NonNull Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatResponse chatResponse = response.body();
                    if (chatResponse.isSuccess()) {
                        callback.onSuccess(chatResponse);
                    } else {
                        String error = chatResponse.getError();
                        callback.onError(error != null ? error : "Unknown error from server");
                    }
                } else {
                    callback.onError("HTTP " + response.code() + ": " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /**
     * Send message with userId for personalized responses
     * @param message User's message
     * @param sessionId Unique session identifier
     * @param userId User ID from Firebase Auth
     * @param callback Callback for response
     */
    public void sendMessageWithUserId(@NonNull String message, @NonNull String sessionId,
                                      @NonNull String userId, @NonNull ChatBotCallback callback) {
        if (message.trim().isEmpty()) {
            callback.onError("Message cannot be empty");
            return;
        }

        ChatRequest request = new ChatRequest(message, sessionId, userId);

        Log.d(TAG, "Sending message with userId: " + userId);

        apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call,
                                 @NonNull Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatResponse chatResponse = response.body();
                    if (chatResponse.isSuccess()) {
                        Log.d(TAG, "Response received: " + chatResponse.getAnswer());
                        callback.onSuccess(chatResponse);
                    } else {
                        String error = chatResponse.getError();
                        callback.onError(error != null ? error : "Unknown error from server");
                    }
                } else {
                    callback.onError("HTTP " + response.code() + ": " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /**
     * Callback interface for chat API responses
     */
    public interface ChatBotCallback {
        void onSuccess(@NonNull ChatResponse response);
        void onError(@NonNull String error);
    }
}

