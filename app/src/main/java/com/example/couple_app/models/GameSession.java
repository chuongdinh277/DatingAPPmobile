package com.example.couple_app.models;

import java.util.HashMap;
import java.util.Map;

public class GameSession {
    private String sessionId;
    private String gameType;
    private Map<String, Object> state;
    private Map<String, Integer> scores;

    public GameSession() {
        this.scores = new HashMap<>();
    }

    public GameSession(String sessionId, String gameType) {
        this.sessionId = sessionId;
        this.gameType = gameType;
        this.state = new HashMap<>();
        this.scores = new HashMap<>();
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public Map<String, Object> getState() { return state; }
    public void setState(Map<String, Object> state) { this.state = state; }

    public Map<String, Integer> getScores() { return scores; }
    public void setScores(Map<String, Integer> scores) { this.scores = scores; }
}
