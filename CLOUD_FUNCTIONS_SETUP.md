# 🚀 Hướng Dẫn Setup Cloud Functions cho Notification

## 📋 Tổng Quan

Cloud Functions đã được tạo với **3 phương thức** gửi notification:

### 1. **TỰ ĐỘNG** (Khuyến nghị) ⭐
- **Function:** `sendNotificationOnNewMessage`
- **Trigger:** Khi có tin nhắn mới trong collection `messages`
- **Ưu điểm:** Tự động, không cần code thêm trong app
- **Cách hoạt động:** App chỉ cần lưu message vào Firestore → Cloud Function tự động gửi notification

### 2. **Gọi Thủ Công**
- **Function:** `sendMessageNotification`
- **Trigger:** Gọi từ app khi cần
- **Ưu điểm:** Kiểm soát hoàn toàn, có thể retry
- **Cách hoạt động:** App gọi Cloud Function trực tiếp qua HTTP Callable

### 3. **Qua Document**
- **Function:** `sendNotificationFromDocument`
- **Trigger:** Khi tạo document trong collection `notifications`
- **Ưu điểm:** Queue-based, dễ retry
- **Cách hoạt động:** App tạo notification document → Cloud Function xử lý

---

## 🛠️ Cài Đặt & Deploy

### Bước 1: Cài đặt Firebase CLI (nếu chưa có)

```cmd
npm install -g firebase-tools
```

### Bước 2: Đăng nhập Firebase

```cmd
firebase login
```

### Bước 3: Khởi tạo project (nếu chưa có)

```cmd
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
firebase init functions
```

**Chọn:**
- Use an existing project → Chọn project của bạn
- JavaScript
- ESLint: No (hoặc Yes nếu muốn)
- Install dependencies: Yes

### Bước 4: Deploy Cloud Functions

```cmd
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
firebase deploy --only functions
```

**Hoặc deploy từng function cụ thể:**

```cmd
firebase deploy --only functions:sendNotificationOnNewMessage
firebase deploy --only functions:sendMessageNotification
firebase deploy --only functions:sendNotificationFromDocument
```

---

## 📱 Cách Sử Dụng Trong Android App

### ✅ Phương Thức 1: TỰ ĐỘNG (Đã implement trong MessengerActivity)

**App KHÔNG CẦN làm gì thêm!** Chỉ cần gửi message như bình thường:

```java
chatManager.sendMessage(coupleId, currentUserId, messageText, new ChatManager.ChatCallback() {
    @Override
    public void onMessageSent() {
        // Xong! Cloud Function sẽ TỰ ĐỘNG gửi notification
        Log.d("MessengerActivity", "Message sent successfully");
    }
});
```

### 🔧 Phương Thức 2: GỌI THỦ CÔNG (Optional - cho trường hợp đặc biệt)

**Thêm dependency vào build.gradle:**

```gradle
implementation 'com.google.firebase:firebase-functions:20.4.0'
```

**Gọi Cloud Function:**

```java
FirebaseFunctions functions = FirebaseFunctions.getInstance();

Map<String, Object> data = new HashMap<>();
data.put("recipientUserId", partnerId);
data.put("title", currentUserName);
data.put("body", messageText);
data.put("coupleId", coupleId);
data.put("senderId", currentUserId);
data.put("senderName", currentUserName);

functions.getHttpsCallable("sendMessageNotification")
    .call(data)
    .addOnSuccessListener(result -> {
        Log.d("Notification", "Sent successfully");
    })
    .addOnFailureListener(e -> {
        Log.e("Notification", "Failed to send", e);
    });
```

### 📝 Phương Thức 3: QUA DOCUMENT (Đã có trong NotificationManager)

**Sử dụng NotificationManager:**

```java
NotificationManager.getInstance().sendMessageNotification(
    partnerId,      // Người nhận
    senderName,     // Tên người gửi
    messageText,    // Nội dung tin nhắn
    coupleId,       // ID couple
    senderId        // ID người gửi
);
```

---

## 🧪 Test Cloud Functions

### Test bằng Firebase Emulator (Local)

```cmd
cd functions
npm install
firebase emulators:start --only functions
```

### Test notification trực tiếp

**Gọi URL test:**
```
https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/testNotification?userId=USER_ID&title=Test&body=Hello
```

### Xem logs

```cmd
firebase functions:log
```

### Xem thống kê notification

```
https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/getNotificationStats
```

---

## 📊 Các Functions Có Sẵn

| Function | Type | Mô tả |
|----------|------|-------|
| `sendNotificationOnNewMessage` | Firestore Trigger | ⭐ TỰ ĐỘNG gửi khi có tin nhắn mới |
| `sendMessageNotification` | HTTP Callable | Gọi thủ công từ app |
| `sendNotificationFromDocument` | Firestore Trigger | Gửi khi có notification document |
| `cleanupOldNotificationLogs` | Scheduled | Dọn log cũ (30 ngày) - 2h sáng |
| `cleanupOldNotifications` | Scheduled | Dọn notification cũ (7 ngày) - 3h sáng |
| `testNotification` | HTTP Request | Test gửi notification |
| `getNotificationStats` | HTTP Request | Xem thống kê notification |

---

## 🔍 Kiểm Tra Hoạt Động

### 1. Kiểm tra Cloud Functions đã deploy

```cmd
firebase functions:list
```

### 2. Kiểm tra logs real-time

```cmd
firebase functions:log --only sendNotificationOnNewMessage
```

### 3. Kiểm tra trong Firebase Console

- Vào Firebase Console → Functions
- Xem danh sách functions đã deploy
- Xem logs và metrics

### 4. Test notification flow

1. **Gửi tin nhắn từ User A**
2. **Kiểm tra logs:**
   ```cmd
   firebase functions:log --only sendNotificationOnNewMessage
   ```
3. **User B nhận được notification**
4. **Kiểm tra collection `notification_logs` trong Firestore**

---

## 🐛 Troubleshooting

### Lỗi: "Function not found"

```cmd
firebase deploy --only functions
```

### Lỗi: "Missing permissions"

Kiểm tra IAM permissions trong Google Cloud Console:
- Firebase Admin SDK Service Account cần có quyền gửi FCM

### Notification không nhận được

**Kiểm tra:**

1. ✅ FCM token đã được lưu trong Firestore?
   ```
   Collection: users/{userId}
   Field: fcmToken
   ```

2. ✅ Cloud Function đã chạy?
   ```cmd
   firebase functions:log
   ```

3. ✅ Message đã được lưu vào Firestore?
   ```
   Collection: messages/{messageId}
   Fields: coupleId, senderId, message
   ```

4. ✅ FirebaseMessagingService đã được đăng ký trong AndroidManifest.xml?
   ```xml
   <service android:name=".services.FirebaseCloudMessagingService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
   ```

### Lỗi: "CORS error" khi test HTTP endpoint

Thêm CORS config:
```javascript
const cors = require('cors')({origin: true});
```

---

## 💰 Chi Phí

### Spark Plan (Free)
- **Invocations:** 125,000/month (đủ cho ~4,000 messages/day)
- **Compute time:** 40,000 GB-seconds/month
- **Network:** 5 GB/month

### Blaze Plan (Pay as you go)
- **Invocations:** $0.40 per million
- Rất rẻ cho app nhỏ và vừa

---

## 📈 Monitoring

### Firestore Collections được tạo

1. **`notification_logs`** - Lưu lịch sử gửi notification
   - Tự động xóa sau 30 ngày
   - Dùng để debugging và analytics

2. **`notifications`** - Queue notification (nếu dùng phương thức 3)
   - Tự động xóa sau 7 ngày khi đã gửi

### Xem thống kê

**API endpoint:**
```
GET https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/getNotificationStats
```

**Response:**
```json
{
  "last24Hours": {
    "total": 150,
    "success": 148,
    "failed": 2,
    "successRate": "98.67%"
  }
}
```

---

## 🎯 Best Practices

1. ✅ **Sử dụng phương thức TỰ ĐỘNG** (sendNotificationOnNewMessage)
   - Đơn giản nhất, ít code nhất
   - Tự động, không quên

2. ✅ **Luôn lưu FCM token** khi app khởi động
   ```java
   FirebaseMessaging.getInstance().getToken()
       .addOnCompleteListener(task -> {
           String token = task.getResult();
           databaseManager.updateUserFcmToken(userId, token, callback);
       });
   ```

3. ✅ **Xử lý token refresh** trong FirebaseMessagingService
   ```java
   @Override
   public void onNewToken(String token) {
       sendRegistrationToServer(token);
   }
   ```

4. ✅ **Test trên thiết bị thật** - Emulator không nhận notification tốt

5. ✅ **Monitor logs** trong giai đoạn đầu để phát hiện lỗi sớm

---

## 🚀 Quick Start - Deploy Ngay

```cmd
# 1. Đảm bảo đang ở thư mục root của project
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp

# 2. Deploy tất cả functions
firebase deploy --only functions

# 3. Xem logs để kiểm tra
firebase functions:log

# 4. Test bằng cách gửi tin nhắn trong app
# Cloud Function sẽ TỰ ĐỘNG gửi notification!
```

---

## ✅ Checklist Hoàn Thành

- [x] File index.js đã tạo với đầy đủ functions
- [x] MessengerActivity đã implement gửi message
- [x] FirebaseMessagingService đã tạo để nhận notification
- [x] FCM token được lưu và update
- [ ] Deploy Cloud Functions lên Firebase
- [ ] Test notification trên thiết bị thật
- [ ] Kiểm tra logs trong Firebase Console

---

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Firebase Console → Functions → Logs
2. Firestore → notification_logs collection
3. `firebase functions:log` trong terminal

**Chúc bạn thành công! 🎉**

