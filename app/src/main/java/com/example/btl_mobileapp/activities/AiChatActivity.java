package com.example.btl_mobileapp.activities;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.adapters.AIChatAdapter;
import com.example.btl_mobileapp.managers.AIChatManager;
import com.example.btl_mobileapp.models.MessageChatBot;
import java.util.Date;

public class AiChatActivity extends AppCompatActivity {

    private static final String TAG = "AIChatActivity";
    private RecyclerView rcChatAI;
    private EditText edtMessage;
    private ImageButton btnSend;

    private AIChatAdapter adapter;
    private String currentUserId = "user";
    private String chatId = "AI";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        rcChatAI = findViewById(R.id.rv_chatAI);
        edtMessage = findViewById(R.id.et_messenger);
        btnSend = findViewById(R.id.bt_sendMessage);
        edtMessage.setText("Bạn cần tôi giúp gì không ?");

        adapter = new AIChatAdapter();

        rcChatAI.setLayoutManager(new LinearLayoutManager(this));
        rcChatAI.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendUserMessage());
    }

    private void sendUserMessage() {
        String text = edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        //Thay đổi hiệu ứng sau khi nhấn gửi và trong thời gian chờ AI trả lời
        edtMessage.setText("AI đang suy nghĩ trả lời câu hỏi ....");
        edtMessage.setFocusable(false);
        btnSend.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop));

        MessageChatBot userMsg = new MessageChatBot(
                currentUserId,
                text,
                new Date().getTime()
        );

        adapter.addMessageUser(userMsg);
        rcChatAI.scrollToPosition(adapter.getItemCount() - 1);

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
                    adapter.addMessageAI(aiMsg);
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

        //Sau khi AI trả lời và render xong thì khôi phục lại hiệu ứng như cũ
        edtMessage.setText("Bạn cần tôi giúp gì không ?");
        edtMessage.setFocusable(true);
        btnSend.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
        ImageViewCompat.setImageTintList(btnSend, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btSend_color)));

    }
}
