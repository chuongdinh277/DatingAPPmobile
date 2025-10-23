package com.example.couple_app.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class để gửi notification qua backend API
 * Thay thế Socket.IO vì Vercel không hỗ trợ WebSocket
 */
public class NotificationAPI {
    private static final String TAG = "NotificationAPI";

    // TODO: Thay đổi URL sau khi deploy lên Vercel
    // Local: http://10.0.2.2:3000 (emulator) hoặc http://192.168.1.x:3000 (real device)
    // Production: https://your-app.vercel.app
    private static final String BASE_URL = "https://noti-qsexeh3uj-anh-nguyens-projects-df238e73.vercel.app";

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface NotificationCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * Gửi notification đến một user qua backend API
     *
     * @param toUserId ID người nhận
     * @param title Tiêu đề notification
     * @param body Nội dung notification
     * @param coupleId ID couple
     * @param fromUserId ID người gửi
     * @param fromUsername Tên người gửi
     * @param callback Callback kết quả
     */
    public static void sendNotification(
            String toUserId,
            String title,
            String body,
            String coupleId,
            String fromUserId,
            String fromUsername,
            NotificationCallback callback
    ) {
        if (toUserId == null || toUserId.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("toUserId is empty"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                // Tạo JSON payload
                JSONObject data = new JSONObject();
                data.put("coupleId", coupleId != null ? coupleId : "");
                data.put("fromUserId", fromUserId != null ? fromUserId : "");
                data.put("fromUsername", fromUsername != null ? fromUsername : "");

                JSONObject payload = new JSONObject();
                payload.put("toUserId", toUserId);
                payload.put("title", title);
                payload.put("body", body);
                payload.put("data", data);

                // Gửi POST request
                String response = sendPostRequest(BASE_URL + "/api/send-notification", payload.toString());

                Log.d(TAG, "✅ Notification sent successfully: " + response);

                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(response));
                }

            } catch (JSONException e) {
                Log.e(TAG, "❌ JSON error: " + e.getMessage(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("JSON error: " + e.getMessage()));
                }
            } catch (IOException e) {
                Log.e(TAG, "❌ Network error: " + e.getMessage(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Gửi HTTP POST request
     */
    private static String sendPostRequest(String urlString, String jsonPayload) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            Log.d(TAG, "📤 Sending request to: " + urlString);
            Log.d(TAG, "📦 Payload: " + jsonPayload);

            // Gửi payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Đọc response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "📥 Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                // Read error response body
                StringBuilder errorResponse = new StringBuilder();
                try {
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(
                            connection.getErrorStream() != null ?
                                connection.getErrorStream() :
                                connection.getInputStream()
                        )
                    );
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error response body", e);
                }

                String errorBody = errorResponse.toString();
                Log.e(TAG, "❌ Error response body: " + errorBody);

                throw new IOException("HTTP error code: " + responseCode +
                    (errorBody.isEmpty() ? "" : ", Response: " + errorBody));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Test connection đến backend
     */
    public static void testConnection(NotificationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "/api/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String result = response.toString();
                    Log.d(TAG, "✅ Backend health check: " + result);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(result));
                    }
                } else {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                connection.disconnect();

            } catch (IOException e) {
                Log.e(TAG, "❌ Backend connection failed: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Connection failed: " + e.getMessage()));
                }
            }
        });
    }
}
