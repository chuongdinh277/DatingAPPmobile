# Chat Backend Server cho Android App

Backend server để xử lý tin nhắn realtime giữa các user trong ứng dụng Android.

## 🚀 Tính năng

- ✅ Gửi tin nhắn riêng tư (1-1) giữa các user
- ✅ Chat nhóm với rooms
- ✅ Hiển thị trạng thái online/offline
- ✅ Typing indicator (đang nhắn tin)
- ✅ Realtime messaging với Socket.IO
- ✅ REST API để lấy thông tin users và rooms
- ✅ Deploy lên Vercel

## 📋 Yêu cầu

- Node.js v14 trở lên
- npm hoặc yarn

## 🛠️ Cài đặt

1. **Clone hoặc tải project**

2. **Cài đặt dependencies:**
```bash
npm install
```

3. **Tạo file .env:**
```bash
cp .env.example .env
```

4. **Chạy server local:**
```bash
# Development mode với nodemon
npm run dev

# hoặc Production mode
npm start
```

Server sẽ chạy tại `http://localhost:3000`

## 📡 API Endpoints

### REST API

- `GET /` - Kiểm tra trạng thái server
- `GET /api/users/online` - Lấy danh sách users đang online
- `GET /api/rooms` - Lấy danh sách rooms

### Socket.IO Events

#### Client → Server Events:

**1. Đăng ký user:**
```javascript
socket.emit('register', {
  userId: 'user123',
  username: 'John Doe'
});
```

**2. Gửi tin nhắn riêng tư:**
```javascript
socket.emit('private_message', {
  toUserId: 'user456',
  fromUserId: 'user123',
  fromUsername: 'John Doe',
  message: 'Hello!'
});
```

**3. Join room (chat nhóm):**
```javascript
socket.emit('join_room', {
  roomId: 'room123',
  userId: 'user123',
  username: 'John Doe'
});
```

**4. Gửi tin nhắn trong room:**
```javascript
socket.emit('room_message', {
  roomId: 'room123',
  fromUserId: 'user123',
  fromUsername: 'John Doe',
  message: 'Hello everyone!'
});
```

**5. Rời khỏi room:**
```javascript
socket.emit('leave_room', {
  roomId: 'room123',
  userId: 'user123',
  username: 'John Doe'
});
```

**6. Typing indicator:**
```javascript
socket.emit('typing', {
  toUserId: 'user456',
  fromUserId: 'user123',
  fromUsername: 'John Doe',
  isTyping: true
});
```

#### Server → Client Events:

**1. User online:**
```javascript
socket.on('user_online', (data) => {
  // data: { userId, username, timestamp }
});
```

**2. User offline:**
```javascript
socket.on('user_offline', (data) => {
  // data: { userId, username, timestamp }
});
```

**3. Danh sách users online:**
```javascript
socket.on('online_users', (users) => {
  // users: Array of user objects
});
```

**4. Nhận tin nhắn riêng tư:**
```javascript
socket.on('private_message', (data) => {
  // data: { fromUserId, fromUsername, message, timestamp }
});
```

**5. Nhận tin nhắn trong room:**
```javascript
socket.on('room_message', (data) => {
  // data: { roomId, fromUserId, fromUsername, message, timestamp }
});
```

**6. Xác nhận tin nhắn đã gửi:**
```javascript
socket.on('message_sent', (data) => {
  // data: { toUserId/roomId, message, timestamp, status }
});
```

**7. Tin nhắn gửi thất bại:**
```javascript
socket.on('message_failed', (data) => {
  // data: { toUserId, reason, timestamp }
});
```

**8. User join room:**
```javascript
socket.on('user_joined_room', (data) => {
  // data: { roomId, userId, username, timestamp }
});
```

**9. User left room:**
```javascript
socket.on('user_left_room', (data) => {
  // data: { roomId, userId, username, timestamp }
});
```

**10. Typing indicator:**
```javascript
socket.on('typing', (data) => {
  // data: { fromUserId, fromUsername, isTyping }
});
```

## 🌐 Deploy lên Vercel

### Cách 1: Deploy với Vercel CLI

1. **Cài đặt Vercel CLI:**
```bash
npm i -g vercel
```

2. **Login vào Vercel:**
```bash
vercel login
```

3. **Deploy:**
```bash
vercel
```

4. **Deploy production:**
```bash
vercel --prod
```

### Cách 2: Deploy qua GitHub

1. Push code lên GitHub repository
2. Truy cập [Vercel Dashboard](https://vercel.com/dashboard)
3. Click "New Project"
4. Import repository từ GitHub
5. Vercel sẽ tự động detect và deploy

## 📱 Tích hợp với Android App

### Thêm Socket.IO client vào Android:

**build.gradle:**
```gradle
dependencies {
    implementation 'io.socket:socket.io-client:2.1.0'
}
```

**Ví dụ code Android (Kotlin):**
```kotlin
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class ChatManager {
    private lateinit var socket: Socket
    
    fun connect(serverUrl: String) {
        socket = IO.socket(serverUrl)
        
        socket.on(Socket.EVENT_CONNECT) {
            // Đăng ký user
            val data = JSONObject()
            data.put("userId", "user123")
            data.put("username", "John Doe")
            socket.emit("register", data)
        }
        
        socket.on("private_message") { args ->
            val data = args[0] as JSONObject
            val message = data.getString("message")
            val from = data.getString("fromUsername")
            // Xử lý tin nhắn nhận được
        }
        
        socket.connect()
    }
    
    fun sendMessage(toUserId: String, message: String) {
        val data = JSONObject()
        data.put("toUserId", toUserId)
        data.put("fromUserId", "user123")
        data.put("fromUsername", "John Doe")
        data.put("message", message)
        socket.emit("private_message", data)
    }
    
    fun disconnect() {
        socket.disconnect()
    }
}
```

## 🔧 Environment Variables

- `PORT` - Port để chạy server (default: 3000)
- `NODE_ENV` - Environment mode (development/production)

## 📝 Lưu ý

⚠️ **Lưu ý về Vercel và Socket.IO:**
- Vercel serverless functions có giới hạn về WebSocket connections
- Đối với production app với nhiều users, nên cân nhắc deploy lên:
  - Railway.app
  - Render.com
  - Heroku
  - VPS riêng

Hoặc sử dụng managed services như:
- Firebase Realtime Database
- Pusher
- Ably

## 🤝 Contributing

Mọi đóng góp đều được chào đón!

## 📄 License

ISC
