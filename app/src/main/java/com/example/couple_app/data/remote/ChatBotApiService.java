package com.example.couple_app.data.remote;

import androidx.annotation.NonNull;

import com.example.couple_app.data.model.ChatRequest;
import com.example.couple_app.data.model.ChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit API service interface for chatbot
 * Defines endpoints for chat communication
 */
public interface ChatBotApiService {

    /**
     * Send chat message to backend
     * @param request ChatRequest containing message, session_id, and user_id
     * @return ChatResponse with answer and metadata
     */
    @POST("chat")
    Call<ChatResponse> sendMessage(@NonNull @Body ChatRequest request);
}

