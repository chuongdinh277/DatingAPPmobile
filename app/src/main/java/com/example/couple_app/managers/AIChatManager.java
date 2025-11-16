package com.example.couple_app.managers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIChatManager {

    private static final String TAG = "AIChatManager";
    private static final String API_URL = "https://rag-chatbot-501013051271.asia-southeast1.run.app"; // Sau khi deploy service được thì chỉnh sửa sau

    public interface AICallback {
        void onToken(String token);
        void onDone(String fullAnswer);
        void onError(String error);
    }

    public static void sendMessageToAI(String userMessage, String userId, String sessionId, AICallback callback) {
        new Thread(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("message", userMessage);
                jsonBody.put("user_id", userId);
                jsonBody.put("session_id", sessionId);

                URL url = new URL(API_URL + "/chat/stream");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode != 200) {
                    callback.onError("HTTP Error: " + responseCode);
                    return;
                }

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"));
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    if (responseLine.startsWith("data: ")) {
                        String data = responseLine.substring(6);
                        if ("[DONE]".equals(data)) {
                            Log.d(TAG, "Stream complete");
                            break;
                        }
                        try {
                            JSONObject json = new JSONObject(data);
                            String type = json.optString("type");
                            if ("start".equals(type)) {
                                Log.d(TAG, "Stream started");
                            } else if ("token".equals(type)) {
                                String content = json.optString("content", "");
                                callback.onToken(content);
                            } else if ("done".equals(type)) {
                                String fullAnswer = json.optString("full_answer", "");
                                callback.onDone(fullAnswer);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Parse error: " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Lỗi : tin nhắn gửi tới service AI không thành công", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
