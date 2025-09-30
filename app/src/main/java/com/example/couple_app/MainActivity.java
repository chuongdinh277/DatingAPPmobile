package com.example.couple_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;
import com.example.couple_app.managers.AuthManager;
import com.example.couple_app.managers.DatabaseManager;
import com.example.couple_app.managers.ChatManager;
import com.example.couple_app.managers.GameManager;
import com.example.couple_app.models.User;
import com.example.couple_app.models.Couple;
import com.example.couple_app.models.ChatMessage;
import com.example.couple_app.models.GameSession;
import com.example.couple_app.utils.CoupleAppUtils;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AuthManager authManager;
    private DatabaseManager databaseManager;
    private ChatManager chatManager;
    private GameManager gameManager;

    private ValueEventListener messageListener;
    private ValueEventListener gameListener;

    // UI components
    private TextView tvUserName, tvUserEmail, tvTestStatus;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        authManager = AuthManager.getInstance();
        databaseManager = DatabaseManager.getInstance();
        chatManager = ChatManager.getInstance();
        gameManager = GameManager.getInstance();

        // Initialize UI components
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvTestStatus = findViewById(R.id.tv_test_status);
        btnLogout = findViewById(R.id.btn_logout);
//
        // Setup logout button
        btnLogout.setOnClickListener(v -> logout());

        // Setup debug button

        // Check if user is already signed in
        if (authManager.isSignedIn()) {
            FirebaseUser currentUser = authManager.getCurrentUser();
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());

            // Update UI with user info
            updateUserInfo(currentUser);

            // Start database test for signed in user
            // testDatabaseOperations();
        } else {
            // Redirect to login screen
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, com.example.couple_app.activities.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateUserInfo(FirebaseUser user) {
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "Chào mừng!";
            } else {
                displayName = "Chào " + displayName + "!";
            }
            tvUserName.setText(displayName);
            tvUserEmail.setText(user.getEmail());
        }
    }

    private void logout() {
        Log.d(TAG, "Logging out user");
        authManager.signOut();

        // Clear any saved data
        CoupleAppUtils.clearCoupleData(this);

        Toast.makeText(this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    // Demo method showing how to sign up a user
    private void demonstrateSignUp() {
        String email = "admin1@gmail.com";
        String password = "admin123";
        String name = "Test User";

        authManager.signUp(email, password, name, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Sign up successful: " + user.getEmail());
                Toast.makeText(MainActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                loadUserData();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Sign up failed: " + error);
                Toast.makeText(MainActivity.this, "Sign up failed: " + error, Toast.LENGTH_LONG).show();

                // If sign up fails (user might already exist), try signing in
                demonstrateSignIn();
            }
        });
    }

    // Demo method showing how to sign in a user
    private void demonstrateSignIn() {
        String email = "admin2@gmail.com";
        String password = "admin123";

        authManager.signIn(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Sign in successful: " + user.getEmail());
                Toast.makeText(MainActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();

                // Start database test after successful sign in
               // testDatabaseOperations();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Sign in failed: " + error);
                Toast.makeText(MainActivity.this, "Sign in failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
//
//    // Comprehensive database test
//    private void testDatabaseOperations() {
//        Log.d(TAG, "=== STARTING DATABASE TESTS ===");
//        Toast.makeText(this, "Starting Database Tests...", Toast.LENGTH_SHORT).show();
//
//        FirebaseUser currentUser = authManager.getCurrentUser();
//        if (currentUser == null) {
//            Log.e(TAG, "No user signed in for database test");
//            return;
//        }
//
//        String userId = currentUser.getUid();
//
//        // Test 1: Read user document from Firestore
//        testFirestoreRead(userId);
//    }
//
//    private void testFirestoreRead(String userId) {
//        Log.d(TAG, "TEST 1: Reading user document from Firestore...");
//
//        databaseManager.getUser(userId, new DatabaseManager.DatabaseCallback<User>() {
//            @Override
//            public void onSuccess(User user) {
//                Log.d(TAG, "✓ TEST 1 PASSED: Successfully read user document");
//                Log.d(TAG, "User data: Name=" + user.getName() + ", Email=" + user.getEmail());
//                Toast.makeText(MainActivity.this, "✓ Firestore READ test passed!", Toast.LENGTH_SHORT).show();
//
//                // Test 2: Update user profile (Firestore write test)
//                testFirestoreWrite(userId);
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 1 FAILED: Failed to read user document - " + error);
//                Toast.makeText(MainActivity.this, "✗ Firestore READ test failed!", Toast.LENGTH_LONG).show();
//
//                // Still continue to next test
//                testFirestoreWrite(userId);
//            }
//        });
//    }
//
//    private void testFirestoreWrite(String userId) {
//        Log.d(TAG, "TEST 2: Writing to Firestore (updating profile)...");
//
//        String testName = "Test User - " + System.currentTimeMillis();
//        String testPhotoUrl = "https://example.com/test-avatar-" + System.currentTimeMillis() + ".jpg";
//
//        databaseManager.updateUserProfile(userId, testName, testPhotoUrl, new AuthManager.AuthActionCallback() {
//            @Override
//            public void onSuccess() {
//                Log.d(TAG, "✓ TEST 2 PASSED: Successfully wrote to Firestore");
//                Log.d(TAG, "Updated profile with name: " + testName);
//                Toast.makeText(MainActivity.this, "✓ Firestore WRITE test passed!", Toast.LENGTH_SHORT).show();
//
//                // Test 3: PIN generation and validation
//                testPinGeneration(userId);
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 2 FAILED: Failed to write to Firestore - " + error);
//                Toast.makeText(MainActivity.this, "✗ Firestore WRITE test failed!", Toast.LENGTH_LONG).show();
//
//                // Continue to next test
//                testPinGeneration(userId);
//            }
//        });
//    }
//
//    private void testPinGeneration(String userId) {
//        Log.d(TAG, "TEST 3: Testing PIN generation...");
//
//        databaseManager.generateAndSavePinForUser(userId, new DatabaseManager.DatabaseCallback<String>() {
//            @Override
//            public void onSuccess(String pin) {
//                Log.d(TAG, "✓ TEST 3 PASSED: Successfully generated and saved PIN: " + pin);
//                Toast.makeText(MainActivity.this, "✓ PIN generation test passed! PIN: " + pin, Toast.LENGTH_LONG).show();
//
//                // Test 4: Realtime Database write test
//                testRealtimeDatabaseWrite(userId);
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 3 FAILED: PIN generation failed - " + error);
//                Toast.makeText(MainActivity.this, "✗ PIN generation test failed!", Toast.LENGTH_LONG).show();
//
//                // Continue to next test
//                testRealtimeDatabaseWrite(userId);
//            }
//        });
//    }
//
//    private void testRealtimeDatabaseWrite(String userId) {
//        Log.d(TAG, "TEST 4: Testing Realtime Database write (sending test message)...");
//
//        // Create a test couple ID for testing
//        String testCoupleId = "test_couple_" + userId;
//        String testMessage = "Test message - " + System.currentTimeMillis();
//
//        chatManager.sendMessage(testCoupleId, userId, testMessage, new ChatManager.ChatCallback() {
//            @Override
//            public void onMessagesReceived(List<ChatMessage> messages) {
//                // Not called for sendMessage
//            }
//
//            @Override
//            public void onMessageSent() {
//                Log.d(TAG, "✓ TEST 4 PASSED: Successfully wrote to Realtime Database");
//                Log.d(TAG, "Sent test message: " + testMessage);
//                Toast.makeText(MainActivity.this, "✓ Realtime Database WRITE test passed!", Toast.LENGTH_SHORT).show();
//
//                // Test 5: Realtime Database read test
//                testRealtimeDatabaseRead(testCoupleId);
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 4 FAILED: Failed to write to Realtime Database - " + error);
//                Toast.makeText(MainActivity.this, "✗ Realtime Database WRITE test failed!", Toast.LENGTH_LONG).show();
//
//                // Continue to next test
//                testRealtimeDatabaseRead(testCoupleId);
//            }
//        });
//    }
//
//    private void testRealtimeDatabaseRead(String testCoupleId) {
//        Log.d(TAG, "TEST 5: Testing Realtime Database read (getting chat history)...");
//
//        chatManager.getChatHistory(testCoupleId, 5, new ChatManager.ChatCallback() {
//            @Override
//            public void onMessagesReceived(List<ChatMessage> messages) {
//                Log.d(TAG, "✓ TEST 5 PASSED: Successfully read from Realtime Database");
//                Log.d(TAG, "Retrieved " + messages.size() + " messages");
//
//                for (int i = 0; i < Math.min(messages.size(), 3); i++) {
//                    ChatMessage msg = messages.get(i);
//                    Log.d(TAG, "Message " + (i+1) + ": " + msg.getMessage());
//                }
//
//                Toast.makeText(MainActivity.this, "✓ Realtime Database READ test passed! Found " + messages.size() + " messages", Toast.LENGTH_SHORT).show();
//
//                // Test 6: Game session test
//                testGameSession(testCoupleId);
//            }
//
//            @Override
//            public void onMessageSent() {
//                // Not called for getChatHistory
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 5 FAILED: Failed to read from Realtime Database - " + error);
//                Toast.makeText(MainActivity.this, "✗ Realtime Database READ test failed!", Toast.LENGTH_LONG).show();
//
//                // Continue to next test
//                testGameSession(testCoupleId);
//            }
//        });
//    }
//
//    private void testGameSession(String testCoupleId) {
//        Log.d(TAG, "TEST 6: Testing Game Session creation...");
//
//        FirebaseUser currentUser = authManager.getCurrentUser();
//        String userId = currentUser.getUid();
//        String dummyPartnerId = "dummy_partner_" + System.currentTimeMillis();
//
//        gameManager.startGameSession(testCoupleId, CoupleAppUtils.GameTypes.QUIZ, userId, dummyPartnerId, new GameManager.GameCallback() {
//            @Override
//            public void onGameCreated(String sessionId) {
//                Log.d(TAG, "✓ TEST 6 PASSED: Successfully created game session: " + sessionId);
//                Toast.makeText(MainActivity.this, "✓ Game session test passed!", Toast.LENGTH_SHORT).show();
//
//                // Test 7: AI Suggestion storage test
//                testAISuggestionStorage(testCoupleId);
//            }
//
//            @Override
//            public void onGameStateUpdated(GameSession gameSession) {
//                // Not called for startGameSession
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 6 FAILED: Failed to create game session - " + error);
//                Toast.makeText(MainActivity.this, "✗ Game session test failed!", Toast.LENGTH_LONG).show();
//
//                // Continue to final test
//                testAISuggestionStorage(testCoupleId);
//            }
//        });
//    }
//
//    private void testAISuggestionStorage(String testCoupleId) {
//        Log.d(TAG, "TEST 7: Testing AI Suggestion storage...");
//
//        String testSuggestion = "Test date suggestion: Go to a nice restaurant - " + System.currentTimeMillis();
//
//        databaseManager.saveAISuggestion(testCoupleId, CoupleAppUtils.SuggestionTypes.DATE_PLAN, testSuggestion, new DatabaseManager.DatabaseActionCallback() {
//            @Override
//            public void onSuccess() {
//                Log.d(TAG, "✓ TEST 7 PASSED: Successfully saved AI suggestion");
//                Toast.makeText(MainActivity.this, "✓ AI Suggestion storage test passed!", Toast.LENGTH_SHORT).show();
//
//                // Final test summary
//                showTestSummary();
//            }
//
//            @Override
//            public void onError(String error) {
//                Log.e(TAG, "✗ TEST 7 FAILED: Failed to save AI suggestion - " + error);
//                Toast.makeText(MainActivity.this, "✗ AI Suggestion storage test failed!", Toast.LENGTH_LONG).show();
//
//                // Show final summary
//                showTestSummary();
//            }
//        });
//    }
//
//    private void showTestSummary() {
//        Log.d(TAG, "=== DATABASE TESTS COMPLETED ===");
//        Log.d(TAG, "Check the logs above for individual test results");
//        Log.d(TAG, "✓ = Test passed, ✗ = Test failed");
//
//        Toast.makeText(this, "🎉 Database tests completed! Check logs for details.", Toast.LENGTH_LONG).show();
//
//        // After tests, load normal user data
//        loadUserData();
//    }

    // Load user data and demonstrate other features
    private void loadUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Get user document from Firestore
        databaseManager.getUser(userId, new DatabaseManager.DatabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "User data loaded: " + user.getName());

                if (user.getPartnerId() == null) {
                    // User doesn't have a partner, demonstrate PIN generation and connection
                    demonstratePinGeneration(userId);
                } else {
                    // User has a partner, demonstrate couple features
                    demonstrateCoupleFeatures(userId, user.getPartnerId());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load user data: " + error);
                Toast.makeText(MainActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Demonstrate PIN generation and connection
    private void demonstratePinGeneration(String userId) {
        // Generate PIN for current user
        databaseManager.generateAndSavePinForUser(userId, new DatabaseManager.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String pin) {
                Log.d(TAG, "PIN generated: " + pin);
                Toast.makeText(MainActivity.this, "Your PIN: " + pin, Toast.LENGTH_LONG).show();
                CoupleAppUtils.saveUserPin(MainActivity.this, pin);

                // For demo purposes, we won't automatically connect
                // In a real app, the partner would enter this PIN to connect
                // demonstratePartnerConnection(userId, pin);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "PIN generation failed: " + error);
                Toast.makeText(MainActivity.this, "PIN generation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Demonstrate partner connection using PIN
    private void demonstratePartnerConnection(String currentUserId, String pin) {
        databaseManager.connectCoupleWithPin(currentUserId, pin, new DatabaseManager.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String coupleId) {
                Log.d(TAG, "Couple connected: " + coupleId);
                Toast.makeText(MainActivity.this, "Connected to partner!", Toast.LENGTH_SHORT).show();
                CoupleAppUtils.saveCoupleId(MainActivity.this, coupleId);

                // Now demonstrate couple features
                demonstrateCoupleFeatures(currentUserId, null);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Connection failed: " + error);
                Toast.makeText(MainActivity.this, "Connection failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Demonstrate couple features (chat, games, love countdown)
    private void demonstrateCoupleFeatures(String userId, String partnerId) {
        // Get couple information
        databaseManager.getCoupleByUserId(userId, new DatabaseManager.DatabaseCallback<Couple>() {
            @Override
            public void onSuccess(Couple couple) {
                Log.d(TAG, "Couple data loaded");
                CoupleAppUtils.saveCoupleId(MainActivity.this, couple.getCoupleId());

                // Calculate and display love days
                long loveDays = databaseManager.calculateLoveDays(couple.getStartDate());
                String loveDaysMessage = CoupleAppUtils.formatLoveDays(loveDays);
                Toast.makeText(MainActivity.this, loveDaysMessage, Toast.LENGTH_LONG).show();

                // Demonstrate chat features
                demonstrateChat(couple.getCoupleId(), userId);

                // Demonstrate game features
                demonstrateGame(couple.getCoupleId(), couple.getUser1Id(), couple.getUser2Id());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load couple data: " + error);
                Toast.makeText(MainActivity.this, "Failed to load couple data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Demonstrate chat functionality
    private void demonstrateChat(String coupleId, String userId) {
        // Send a test message
        chatManager.sendMessage(coupleId, userId, "Hello from the app!", new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                // This won't be called for sendMessage
            }

            @Override
            public void onMessageSent() {
                Log.d(TAG, "Message sent successfully");
                Toast.makeText(MainActivity.this, "Message sent!", Toast.LENGTH_SHORT).show();

                // Get chat history
                getChatHistory(coupleId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to send message: " + error);
            }
        });

        // Listen for new messages
        messageListener = chatManager.listenForMessages(coupleId, new ChatManager.MessageListener() {
            @Override
            public void onNewMessage(ChatMessage message) {
                Log.d(TAG, "New message received: " + message.getMessage());
                String time = CoupleAppUtils.formatChatTimestamp(message.getTimestamp());
                Toast.makeText(MainActivity.this, "New message at " + time, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Message listener error: " + error);
            }
        });
    }

    // Get chat history
    private void getChatHistory(String coupleId) {
        chatManager.getChatHistory(coupleId, 10, new ChatManager.ChatCallback() {
            @Override
            public void onMessagesReceived(List<ChatMessage> messages) {
                Log.d(TAG, "Loaded " + messages.size() + " messages");
                for (ChatMessage message : messages) {
                    Log.d(TAG, "Message: " + message.getMessage());
                }
            }

            @Override
            public void onMessageSent() {
                // This won't be called for getChatHistory
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load chat history: " + error);
            }
        });
    }

    // Demonstrate game functionality
    private void demonstrateGame(String coupleId, String user1Id, String user2Id) {
        // Start a quiz game
        gameManager.startGameSession(coupleId, CoupleAppUtils.GameTypes.QUIZ, user1Id, user2Id, new GameManager.GameCallback() {
            @Override
            public void onGameCreated(String sessionId) {
                Log.d(TAG, "Game session created: " + sessionId);
                Toast.makeText(MainActivity.this, "Quiz game started!", Toast.LENGTH_SHORT).show();

                // Listen for game state changes
                gameListener = gameManager.listenToGameState(coupleId, sessionId, new GameManager.GameStateListener() {
                    @Override
                    public void onGameStateChanged(GameSession gameSession) {
                        Log.d(TAG, "Game state changed: " + gameSession.getGameType());
                        // Handle game state updates in UI
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Game listener error: " + error);
                    }
                });

                // Simulate answering a question
                simulateGamePlay(coupleId, sessionId, user1Id);
            }

            @Override
            public void onGameStateUpdated(GameSession gameSession) {
                Log.d(TAG, "Game state updated");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to create game: " + error);
            }
        });
    }

    // Simulate game play
    private void simulateGamePlay(String coupleId, String sessionId, String playerId) {
        // Simulate answering a quiz question correctly
        gameManager.answerQuizQuestion(coupleId, sessionId, playerId, 0, "Paris", true, new GameManager.GameCallback() {
            @Override
            public void onGameCreated(String sessionId) {}

            @Override
            public void onGameStateUpdated(GameSession gameSession) {
                Log.d(TAG, "Quiz question answered, score updated");
                Integer score = gameSession.getScores().get(playerId);
                Toast.makeText(MainActivity.this, "Your score: " + (score != null ? score : 0), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to answer question: " + error);
            }
        });
    }

    // Demonstrate profile update
    private void demonstrateProfileUpdate() {
        authManager.updateProfile("Updated Name", "https://example.com/avatar.jpg", new AuthManager.AuthActionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Profile updated successfully");
                Toast.makeText(MainActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Profile update failed: " + error);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listeners to prevent memory leaks
        String coupleId = CoupleAppUtils.getCoupleId(this);
        if (coupleId != null) {
            if (messageListener != null) {
                chatManager.removeMessageListener(coupleId, messageListener);
            }
            // Note: You would need to store sessionId to remove game listener
            // gameManager.removeGameStateListener(coupleId, sessionId, gameListener);
        }
    }
}
