package com.example.couple_app.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.couple_app.R;
import com.example.couple_app.ui.adapters.ChatSessionAdapter;
import com.example.couple_app.data.model.ChatSession;
import com.example.couple_app.data.repository.ChatSessionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.util.Log;

import java.util.List;

/**
 * Activity for viewing chat session history
 * Displays all past conversations with the chatbot
 */
public class ChatHistoryActivity extends AppCompatActivity implements ChatSessionAdapter.OnSessionClickListener {

    private static final String TAG = "ChatHistoryActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View tvEmpty;
    private ChatSessionAdapter adapter;
    private ChatSessionRepository repository;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử Chat");
        }

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        // Initialize views
        recyclerView = findViewById(R.id.recycler_chat_history);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        // Setup RecyclerView
        adapter = new ChatSessionAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Initialize repository
        repository = new ChatSessionRepository();

        // Load chat history
        loadChatHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadChatHistory();
    }

    private void loadChatHistory() {
        loadChatHistory(false);
    }

    private void loadChatHistory(boolean useFallback) {
        showLoading(true);
        Log.d(TAG, "Loading chat history for user: " + currentUserId + " (fallback=" + useFallback + ")");

        ChatSessionRepository.OnSessionsLoadedListener listener = new ChatSessionRepository.OnSessionsLoadedListener() {
            @Override
            public void onSessionsLoaded(List<ChatSession> sessions) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Log.d(TAG, "Loaded " + sessions.size() + " sessions");
                    adapter.setSessions(sessions);

                    if (sessions.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading sessions: " + error);

                    // If index error and haven't tried fallback yet, use fallback automatically
                    if (!useFallback && error.contains("index")) {
                        Log.w(TAG, "Index not ready, using fallback method");
                        Toast.makeText(ChatHistoryActivity.this,
                                "Index chưa sẵn sàng, đang dùng phương thức thay thế...",
                                Toast.LENGTH_SHORT).show();
                        loadChatHistory(true); // Retry with fallback
                        return;
                    }

                    showEmpty(true);

                    // Show detailed error in AlertDialog
                    new AlertDialog.Builder(ChatHistoryActivity.this)
                            .setTitle("Không thể tải lịch sử")
                            .setMessage(error)
                            .setPositiveButton("Thử lại", (dialog, which) -> {
                                loadChatHistory(false); // Retry with normal method
                            })
                            .setNeutralButton("Dùng phương thức khác", (dialog, which) -> {
                                loadChatHistory(true); // Try with fallback
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                });
            }
        };

        // Use fallback method if requested, otherwise use normal method
        if (useFallback) {
            repository.getUserSessionsNoIndex(currentUserId, listener);
        } else {
            repository.getUserSessions(currentUserId, listener);
        }
    }

    @Override
    public void onSessionClick(ChatSession session) {
        // Open chatbot and resume the selected session directly
        Intent intent = new Intent(this, HomeMainActivity.class);
        intent.putExtra("resume_session_id", session.getSessionId());
        intent.putExtra("resume_session_title", session.getDisplayTitle());
        // Ensure we return to home and open the bottom sheet; HomeMainActivity FAB click opens it,
        // but here we just pass extras and let HomeMainActivity open the sheet from FAB click.
        // Alternatively, we can trigger open immediately by adding a flag handled in onResume.
        startActivity(intent);
        // Optional: close history so back goes to home
        finish();
    }

    @Override
    public void onSessionDelete(ChatSession session, int position) {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Xóa cuộc trò chuyện?")
                .setMessage("Bạn có chắc muốn xóa cuộc trò chuyện này? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteSession(session, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onSessionEdit(ChatSession session, int position) {
        // Show dialog to edit session title
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi tên cuộc trò chuyện");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(session.getTitle());
        input.setHint("Nhập tên mới...");
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                updateSessionTitle(session, newTitle, position);
            } else {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", null);

        builder.show();
    }

    private void deleteSession(ChatSession session, int position) {
        progressBar.setVisibility(View.VISIBLE);

        repository.deleteSession(session.getSessionId(), new ChatSessionRepository.OnSessionOperationListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.removeSession(position);
                    Toast.makeText(ChatHistoryActivity.this,
                            "Đã xóa cuộc trò chuyện",
                            Toast.LENGTH_SHORT).show();

                    // Check if list is now empty
                    if (adapter.getItemCount() == 0) {
                        showEmpty(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ChatHistoryActivity.this,
                            "Lỗi xóa: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateSessionTitle(ChatSession session, String newTitle, int position) {
        progressBar.setVisibility(View.VISIBLE);

        repository.updateSessionTitle(session.getSessionId(), newTitle,
                new ChatSessionRepository.OnSessionOperationListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    session.setTitle(newTitle);
                    adapter.updateSession(position, session);
                    Toast.makeText(ChatHistoryActivity.this,
                            "Đã cập nhật tên",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ChatHistoryActivity.this,
                            "Lỗi cập nhật: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
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
