package com.example.btl_mobileapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.AIChatAdapter;
import com.example.btl_mobileapp.managers.AIChatManager;
import com.example.btl_mobileapp.managers.ChatMemoryManager;
import com.example.btl_mobileapp.models.MessageChatBot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AiChatActivity extends AppCompatActivity {

    private static final String TAG = "AIChatActivity";
    private RecyclerView rcChatAI;
    private EditText edtMessage;
    private ImageButton btnSend;

    private AIChatAdapter adapter;
    private List<MessageChatBot> messages;
    private String currentUserId = "user";
    private String chatId = "ai_chat_session";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        rcChatAI = findViewById(R.id.rv_chatAI);
        edtMessage = findViewById(R.id.et_messenger);
        btnSend = findViewById(R.id.bt_sendMessage);

        messages = new ArrayList<>(ChatMemoryManager.getInstance().getMessages(chatId));
        adapter = new AIChatAdapter(messages, currentUserId);

        rcChatAI.setLayoutManager(new LinearLayoutManager(this));
        rcChatAI.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendUserMessage());
    }

    private void sendUserMessage() {
        String text = edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // Tạo tin nhắn của user
        MessageChatBot userMsg = new MessageChatBot(
                currentUserId,
                text,
                new Date().getTime()
        );

        // Cập nhật UI và lưu bộ nhớ
        adapter.addMessage(userMsg);
        ChatMemoryManager.getInstance().addMessage(chatId, userMsg);
        rcChatAI.scrollToPosition(adapter.getItemCount() - 1);
        edtMessage.setText("");

        // Gửi lên AI
        AIChatManager.sendMessageToAI(text, new AIChatManager.AICallback() {
            @Override
            public void onSuccess(String aiResponse) {
                runOnUiThread(() -> {
                    MessageChatBot aiMsg = new MessageChatBot(
                            chatId,
                            aiResponse,
                            new Date().getTime()
                    );
                    adapter.addMessage(aiMsg);
                    ChatMemoryManager.getInstance().addMessage(chatId, aiMsg);
                    rcChatAI.scrollToPosition(adapter.getItemCount() - 1);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(AiChatActivity.this, "Lỗi khi gửi: " + error, Toast.LENGTH_SHORT).show()
                );
                Log.e(TAG, "AI Error: " + error);
            }
        });
    }
}
