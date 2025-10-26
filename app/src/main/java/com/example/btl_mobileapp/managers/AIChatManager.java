package com.example.btl_mobileapp.managers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIChatManager {

    private static final String TAG = "AIChatManager";
    private static final String API_URL = "URL của service AI"; // Sau khi deploy service được thì chỉnh sửa sau

    public interface AICallback {
        void onSuccess(String aiResponse);
        void onError(String error);
    }

    public static void sendMessageToAI(String userMessage, AICallback callback) {
        new Thread(() -> {
            try {
                ChatMemoryManager memory = ChatMemoryManager.getInstance();
                JSONObject jsonBody = new JSONObject();
                JSONArray jsonHistory = new JSONArray();
                jsonBody.put("question", userMessage);

                // Biến value để lấy giá trị index min của i bởi vì hệ thống AI sẽ chỉ lấy và nhớ 5 lần trò chuyện của người dùng và AI
                int value = (memory.getMessages("AI").size() > 5) ? memory.getMessages("AI").size() - 5 : 0;
                for (int i = memory.getMessages("AI").size() - 1; i >= value; i--) {
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("user", memory.getMessages("user").get(i).getContent());
                    jsonMessage.put("AI", memory.getMessages("AI").get(i).getContent());
                    jsonHistory.put(jsonMessage);
                }

                jsonBody.put("History", jsonHistory);

                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject responseJson = new JSONObject(response.toString());
                String aiReply = responseJson.optString("answer", "Xin lỗi, Câu hỏi của bạn nằm ngoài phạm vi của tôi.");

                callback.onSuccess(aiReply);

            } catch (Exception e) {
                Log.e(TAG, "Lỗi : tin nhắn gửi tới service AI không thành công", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
