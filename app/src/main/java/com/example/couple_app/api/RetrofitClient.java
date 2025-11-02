package com.example.couple_app.api;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client singleton for chatbot API
 * Handles HTTP communication with backend service
 */
public class RetrofitClient {

    // TODO: Replace with actual Cloud Run URL
    // Get URL by running: gcloud run services describe rag-chatbot --region asia-southeast1 --format="value(status.url)"
    private static final String BASE_URL = "https://rag-chatbot-501013051271.asia-southeast1.run.app/";

    private static RetrofitClient instance;
    private final ChatBotApiService apiService;

    private RetrofitClient() {
        // Logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp client with timeout configuration
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)  // Longer timeout for Cloud Run
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        // Build Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ChatBotApiService.class);
    }

    @NonNull
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    @NonNull
    public ChatBotApiService getApiService() {
        return apiService;
    }
}

