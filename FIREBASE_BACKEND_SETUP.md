# Couple App Firebase Backend Setup - Complete Implementation

## Overview
This document provides a complete Firebase backend integration for your Android couples app built in Java. The implementation includes user authentication, real-time messaging, game sessions, and structured data storage.

## Project Structure

### Core Components
1. **CoupleApplication.java** - Application class that initializes Firebase
2. **Manager Classes** - Handle all Firebase operations
   - AuthManager.java - User authentication operations
   - DatabaseManager.java - Firestore operations
   - ChatManager.java - Real-time messaging via Realtime Database
   - GameManager.java - Real-time game sessions
3. **Model Classes** - Data structures for Firebase documents
   - User.java, Couple.java, Story.java, AISuggestion.java
   - ChatMessage.java, GameSession.java
4. **Utility Classes**
   - CoupleAppUtils.java - Helper methods and constants

### Firebase Configuration
- Firebase BOM version: 33.1.2
- Services: Authentication, Firestore, Realtime Database
- Google Services plugin configured

## Key Features Implemented

### 1. User Authentication
``` java
// Sign up
AuthManager.getInstance().signUp(email, password, name, callback);

// Sign in
AuthManager.getInstance().signIn(email, password, callback);

// Update profile
AuthManager.getInstance().updateProfile(name, photoUrl, callback);
``` 

### 2. PIN-based Partner Connection
``` java
// Generate unique PIN
DatabaseManager.getInstance().generateAndSavePinForUser(userId, callback);

// Connect using partner's PIN
DatabaseManager.getInstance().connectCoupleWithPin(currentUserId, pin, callback);
```

### 3. Real-time Chat
``` java
// Send message
ChatManager.getInstance().sendMessage(coupleId, senderId, message, callback);

// Listen for new messages
ValueEventListener listener = ChatManager.getInstance().listenForNewMessages(coupleId, messageListener);
```

### 4. Game Sessions
``` java
// Start game
GameManager.getInstance().startGameSession(coupleId, gameType, user1Id, user2Id, callback);

// Update game state
GameManager.getInstance().updateGameState(coupleId, sessionId, updates, callback);

// Listen for game changes
ValueEventListener listener = GameManager.getInstance().listenToGameState(coupleId, sessionId, stateListener);
```

### 5. Love Day Counter
``` java
// Calculate days since relationship started
long days = DatabaseManager.getInstance().calculateLoveDays(startDate);
String message = CoupleAppUtils.formatLoveDays(days);
```

## Database Structure

### Firestore Collections

#### users/{userId}
``` json
{
  "userId": "string",
  "name": "string",
  "email": "string", 
  "profilePicUrl": "string",
  "pinCode": "string (6-digit)",
  "partnerId": "string",
  "startLoveDate": "timestamp"
}
```

#### couples/{coupleId}
``` json
{
  "user1Id": "string",
  "user2Id": "string",
  "startDate": "timestamp",
  "sharedStories": [
    {
      "text": "string",
      "timestamp": "timestamp"
    }
  ]
}
```

#### ai_suggestions/{suggestionId}
``` json
{
  "coupleId": "string",
  "type": "date_plan | gift",
  "suggestionText": "string",
  "timestamp": "timestamp"
}
```

### Realtime Database Structure

#### /chats/{coupleId}/{messageId}
``` json
{
  "senderId": "string",
  "message": "string",
  "timestamp": "server_timestamp"
}
```

#### /games/{coupleId}/{sessionId}
``` json
{
  "gameType": "string",
  "state": {
    "currentQuestion": "number",
    "gameStarted": "boolean",
    // ... game-specific state
  },
  "scores": {
    "user1Id": "number",
    "user2Id": "number"
  }
}
```

## Security Rules

### Firestore Rules
- Users can only access their own user document
- Couples can only access their shared couple document
- AI suggestions are restricted to the couple they belong to

### Realtime Database Rules
- Chat messages are restricted to coupled users only
- Game sessions are restricted to the couple participants
- Write operations validate message structure and sender authentication

## Usage Examples

### Complete User Flow
1. **Sign Up/Sign In**
   ``` java
   AuthManager.getInstance().signUp(email, password, name, callback);
   ```

2. **Generate PIN for Connection**
   ``` java
   DatabaseManager.getInstance().generateAndSavePinForUser(userId, callback);
   ```

3. **Partner Connects Using PIN**
   ``` java
   DatabaseManager.getInstance().connectCoupleWithPin(userId, pin, callback);
   ```

4. **Start Chatting**
   ``` java
   ChatManager.getInstance().sendMessage(coupleId, userId, "Hello!", callback);
   ```

5. **Play Games Together**
   ``` java
   GameManager.getInstance().startGameSession(coupleId, "quiz", user1Id, user2Id, callback);
   ```

## Error Handling
- All operations include comprehensive error handling
- Network connectivity issues are managed
- Invalid data validation
- User-friendly error messages

## Best Practices Implemented
- Singleton pattern for managers
- Proper listener cleanup to prevent memory leaks
- Input validation for all user data
- Secure PIN generation with uniqueness checking
- Real-time synchronization for collaborative features

## Setup Instructions
1. Add your `google-services.json` file to the app directory
2. Configure Firebase project with Authentication, Firestore, and Realtime Database
3. Apply the provided security rules to both databases
4. Build and run the application

## Testing
The MainActivity includes demonstration code that:
- Creates a test user account
- Generates a PIN
- Sends test messages
- Creates a game session
- Shows love day calculation

All operations are logged and display Toast messages for verification.

## Next Steps
1. Create UI activities for each feature
2. Implement Cloud Functions for AI suggestions
3. Add push notifications for messages
4. Implement image sharing in chat
5. Add more game types
6. Create profile management screens

The backend is now fully functional and ready for frontend development!
