package com.example.couple_app.managers;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.example.couple_app.data.model.GameSession;
import java.util.HashMap;
import java.util.Map;

public class GameManager {
    private static final String TAG = "GameManager";
    private DatabaseReference database;
    private static GameManager instance;

    // Database paths
    private static final String GAMES_PATH = "games";

    private GameManager() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public interface GameCallback {
        void onGameCreated(String sessionId);
        void onGameStateUpdated(GameSession gameSession);
        void onError(String error);
    }

    public interface GameStateListener {
        void onGameStateChanged(GameSession gameSession);
        void onError(String error);
    }

    // Start a new game session
    public void startGameSession(String coupleId, String gameType, String user1Id, String user2Id, GameCallback callback) {
        if (coupleId == null || gameType == null || user1Id == null || user2Id == null) {
            callback.onError("Invalid game parameters");
            return;
        }

        DatabaseReference gameRef = database.child(GAMES_PATH).child(coupleId);
        DatabaseReference newSessionRef = gameRef.push();

        GameSession gameSession = new GameSession(newSessionRef.getKey(), gameType);

        // Initialize scores
        Map<String, Integer> scores = new HashMap<>();
        scores.put(user1Id, 0);
        scores.put(user2Id, 0);
        gameSession.setScores(scores);

        // Initialize game state based on game type
        Map<String, Object> initialState = createInitialGameState(gameType);
        gameSession.setState(initialState);

        newSessionRef.setValue(gameSession)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Game session created successfully");
                callback.onGameCreated(newSessionRef.getKey());
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error creating game session", e);
                callback.onError("Failed to create game session: " + e.getMessage());
            });
    }

    // Create initial game state based on game type
    private Map<String, Object> createInitialGameState(String gameType) {
        Map<String, Object> state = new HashMap<>();

        switch (gameType.toLowerCase()) {
            case "quiz":
                state.put("currentQuestion", 0);
                state.put("totalQuestions", 10);
                state.put("currentTurn", "");
                state.put("questionAnswered", false);
                break;

            case "puzzle":
                state.put("puzzleCompleted", false);
                state.put("pieces", new HashMap<String, Object>());
                state.put("currentPlayer", "");
                break;

            case "memory":
                state.put("cards", new HashMap<String, Object>());
                state.put("flippedCards", new HashMap<String, Object>());
                state.put("currentTurn", "");
                state.put("matches", 0);
                break;

            default:
                state.put("gameStarted", true);
                state.put("currentTurn", "");
                break;
        }

        return state;
    }

    // Update game state
    public void updateGameState(String coupleId, String sessionId, Map<String, Object> stateUpdates, GameCallback callback) {
        if (coupleId == null || sessionId == null || stateUpdates == null) {
            callback.onError("Invalid update parameters");
            return;
        }

        DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);

        // Update only the state field
        sessionRef.child("state").updateChildren(stateUpdates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Game state updated successfully");
                // Get updated game session
                getGameSession(coupleId, sessionId, callback);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error updating game state", e);
                callback.onError("Failed to update game state: " + e.getMessage());
            });
    }

    // Update player score
    public void updatePlayerScore(String coupleId, String sessionId, String playerId, int newScore, GameCallback callback) {
        if (coupleId == null || sessionId == null || playerId == null) {
            callback.onError("Invalid score update parameters");
            return;
        }

        DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);

        sessionRef.child("scores").child(playerId).setValue(newScore)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Player score updated successfully");
                // Get updated game session
                getGameSession(coupleId, sessionId, callback);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error updating player score", e);
                callback.onError("Failed to update score: " + e.getMessage());
            });
    }

    // Get current game session
    public void getGameSession(String coupleId, String sessionId, GameCallback callback) {
        if (coupleId == null || sessionId == null) {
            callback.onError("Invalid session parameters");
            return;
        }

        DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);

        sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GameSession gameSession = dataSnapshot.getValue(GameSession.class);
                if (gameSession != null) {
                    gameSession.setSessionId(dataSnapshot.getKey());
                    callback.onGameStateUpdated(gameSession);
                } else {
                    callback.onError("Game session not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Error getting game session", error.toException());
                callback.onError("Failed to get game session: " + error.getMessage());
            }
        });
    }

    // Listen for game state changes in real-time
    public ValueEventListener listenToGameState(String coupleId, String sessionId, GameStateListener listener) {
        if (coupleId == null || sessionId == null) {
            listener.onError("Invalid session parameters");
            return null;
        }

        DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);

        ValueEventListener gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GameSession gameSession = dataSnapshot.getValue(GameSession.class);
                if (gameSession != null) {
                    gameSession.setSessionId(dataSnapshot.getKey());
                    listener.onGameStateChanged(gameSession);
                } else {
                    listener.onError("Game session not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Game state listener cancelled", error.toException());
                listener.onError("Failed to listen to game state: " + error.getMessage());
            }
        };

        sessionRef.addValueEventListener(gameListener);
        return gameListener;
    }

    // Remove game state listener
    public void removeGameStateListener(String coupleId, String sessionId, ValueEventListener listener) {
        if (coupleId != null && sessionId != null && listener != null) {
            DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);
            sessionRef.removeEventListener(listener);
        }
    }

    // End game session
    public void endGameSession(String coupleId, String sessionId, GameCallback callback) {
        if (coupleId == null || sessionId == null) {
            callback.onError("Invalid session parameters");
            return;
        }

        DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);

        // Mark game as ended
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameEnded", true);
        updates.put("endTime", System.currentTimeMillis());

        sessionRef.child("state").updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Game session ended successfully");
                getGameSession(coupleId, sessionId, callback);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error ending game session", e);
                callback.onError("Failed to end game session: " + e.getMessage());
            });
    }

    // Delete game session
    public void deleteGameSession(String coupleId, String sessionId, GameCallback callback) {
        if (coupleId == null || sessionId == null) {
            callback.onError("Invalid session parameters");
            return;
        }

        DatabaseReference sessionRef = database.child(GAMES_PATH).child(coupleId).child(sessionId);

        sessionRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Game session deleted successfully");
                callback.onGameCreated(""); // Reuse callback to indicate success
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error deleting game session", e);
                callback.onError("Failed to delete game session: " + e.getMessage());
            });
    }

    // Helper methods for specific game types

    // Quiz game helpers
    public void answerQuizQuestion(String coupleId, String sessionId, String playerId, int questionIndex, String answer, boolean isCorrect, GameCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentQuestion", questionIndex + 1);
        updates.put("lastAnswer", answer);
        updates.put("lastAnswerCorrect", isCorrect);
        updates.put("lastAnswerBy", playerId);
        updates.put("questionAnswered", true);

        updateGameState(coupleId, sessionId, updates, new GameCallback() {
            @Override
            public void onGameCreated(String sessionId) {}

            @Override
            public void onGameStateUpdated(GameSession gameSession) {
                // Update score if answer is correct
                if (isCorrect) {
                    Integer currentScore = gameSession.getScores().get(playerId);
                    int newScore = (currentScore != null ? currentScore : 0) + 1;
                    updatePlayerScore(coupleId, sessionId, playerId, newScore, callback);
                } else {
                    callback.onGameStateUpdated(gameSession);
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // Memory game helpers
    public void flipMemoryCard(String coupleId, String sessionId, String playerId, int cardIndex, GameCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("flippedCards." + cardIndex, playerId);
        updates.put("currentTurn", playerId);

        updateGameState(coupleId, sessionId, updates, callback);
    }
}
