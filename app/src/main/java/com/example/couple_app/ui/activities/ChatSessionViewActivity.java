package com.example.couple_app.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.ui.adapters.ChatMessageAdapter;
import com.example.couple_app.data.model.ChatBotMessage;
import com.example.couple_app.data.model.ChatSessionMessage;
import com.example.couple_app.data.repository.ChatSessionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for viewing a specific chat session
 * Displays the full conversation history in read-only mode
 */
public class ChatSessionViewActivity extends AppCompatActivity {

    private static final String TAG = "ChatSessionView";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ChatMessageAdapter adapter;
    private ChatSessionRepository repository;

    private String sessionId;
    private String sessionTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_session_view);

        // Get session info from intent
        sessionId = getIntent().getStringExtra("session_id");
        sessionTitle = getIntent().getStringExtra("session_title");

        if (sessionId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(sessionTitle != null ? sessionTitle : "Chi tiết Chat");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_chat_messages);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        // Setup RecyclerView
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Initialize repository
        repository = new ChatSessionRepository();

        // Load messages
        loadMessages();
    }

    private void loadMessages() {
        showLoading(true);

        repository.getSessionMessages(sessionId, new ChatSessionRepository.OnMessagesLoadedListener() {
            @Override
            public void onMessagesLoaded(List<ChatSessionMessage> messages) {
                runOnUiThread(() -> {
                    showLoading(false);

                    if (messages.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        displayMessages(messages);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ChatSessionViewActivity.this,
                            "Lỗi tải tin nhắn: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayMessages(List<ChatSessionMessage> sessionMessages) {
        List<ChatBotMessage> chatBotMessages = new ArrayList<>();
        for (ChatSessionMessage msg : sessionMessages) {
            chatBotMessages.add(msg.toChatBotMessage());
        }
        adapter.setMessages(chatBotMessages);

        // Scroll to bottom
        recyclerView.postDelayed(() -> {
            if (adapter.getItemCount() > 0) {
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        }, 100);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

