# 🔧 Hướng Dẫn Sửa Lỗi: Duplicate Tin Nhắn & Không Nhận Notification

## ✅ Đã Sửa

### 1️⃣ Lỗi Duplicate Tin Nhắn
**Nguyên nhân:** Real-time listener có thể nhận lại tin nhắn vừa gửi

**Giải pháp:** Thêm check `isMessageAlreadyExists()` để kiểm tra messageId trước khi thêm vào list

**Code đã thêm vào MessengerActivity:**
```java
private boolean isMessageAlreadyExists(String messageId) {
    if (messageId == null || messageId.isEmpty()) {
        return false;
    }
    
    for (Message msg : messageList) {
        if (messageId.equals(msg.getMessageId())) {
            return true;
        }
    }
    return false;
}
```

### 2️⃣ Lỗi Không Nhận Notification
**Nguyên nhân:** ❌ **SAI SERVER KEY!**

Bạn đang dùng:
```
AIzaSyC0LJ8z3y7EhDtql52itqTwNRyIC5sc53M  ← Web API Key (SAI!)
```

Cần dùng:
```
AAAA... ← Server Key (ĐÚNG!)
```

---

## 🔑 Cách Lấy Server Key ĐÚNG

### Bước 1: Vào Firebase Console
https://console.firebase.google.com/

### Bước 2: Chọn Project của bạn
Click vào project "Couples App"

### Bước 3: Vào Settings
Click vào icon ⚙️ → **Project settings**

### Bước 4: Tab Cloud Messaging
Click tab **Cloud Messaging**

### Bước 5: Enable Legacy API (Nếu cần)
Nếu không thấy "Server key", tìm phần:
```
Cloud Messaging API (Legacy)
```

Click **Enable Cloud Messaging API (Legacy)**

### Bước 6: Copy Server Key
Copy **Server key** - nó sẽ bắt đầu bằng `AAAA...`

**Ví dụ Server Key ĐÚNG:**
```
AAAAabcdefg:APA91bGxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

## 🛠️ Cách Cập Nhật Server Key

### Mở file:
```
app/src/main/java/com/example/couple_app/utils/FCMNotificationSender.java
```

### Tìm dòng 41:
```java
private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE";
```

### Thay bằng Server Key của bạn:
```java
private static final String SERVER_KEY = "AAAAabcdefg:APA91b..."; // Server Key từ Firebase Console
```

⚠️ **LƯU Ý:** 
- Server Key phải bắt đầu bằng `AAAA...`
- KHÔNG dùng Web API Key (bắt đầu bằng `AIza...`)

---

## 🧪 Kiểm Tra Sau Khi Sửa

### 1. Build lại app
```cmd
gradlew clean assembleDebug
```

### 2. Cài trên 2 thiết bị

### 3. Test gửi tin nhắn

**Kết quả mong đợi:**
- ✅ Tin nhắn KHÔNG bị duplicate
- ✅ Máy đối phương NHẬN ĐƯỢC notification
- ✅ Notification hiển thị tên người gửi và nội dung

---

## 📊 Kiểm Tra Logs

### Logcat Filter: FCMNotificationSender

**Nếu thấy:**
```
✅ Notification sent successfully: Success: 200
```
→ Thành công!

**Nếu thấy:**
```
❌ HTTP error code: 401
```
→ Server Key sai, kiểm tra lại

**Nếu thấy:**
```
⚠️ SERVER_KEY chưa được cấu hình
```
→ Chưa thay YOUR_SERVER_KEY_HERE

---

## 🐛 Troubleshooting

### Vẫn bị duplicate?
**Kiểm tra:**
1. Đã rebuild app chưa?
2. Xóa app và cài lại
3. Clear app data

### Vẫn không nhận notification?

**Checklist:**
- [ ] Server Key đúng (bắt đầu bằng AAAA)?
- [ ] Cloud Messaging API (Legacy) đã enable?
- [ ] FCM token đã lưu trong Firestore?
- [ ] Permission POST_NOTIFICATIONS đã grant? (Android 13+)
- [ ] FirebaseMessagingService đã đăng ký trong AndroidManifest?

**Kiểm tra FCM token:**
1. Vào Firestore trong Firebase Console
2. Collection: `users`
3. Document: `{userId}`
4. Field: `fcmToken` phải có giá trị

**Test FCM token thủ công:**
Vào: https://console.firebase.google.com/project/YOUR_PROJECT/notification

Click "Send test message" và paste FCM token để test.

---

## 🎯 So Sánh Web API Key vs Server Key

| | Web API Key | Server Key |
|---|---|---|
| **Bắt đầu bằng** | `AIza...` | `AAAA...` |
| **Dùng cho** | Web apps, Maps, etc. | FCM notifications |
| **Hoạt động với FCM?** | ❌ KHÔNG | ✅ CÓ |
| **Bạn đang dùng** | ✅ (Sai!) | ❌ (Cần dùng) |

---

## 📝 Tóm Tắt

### Đã sửa:
1. ✅ **Duplicate tin nhắn** - Thêm check messageId
2. ✅ **Không có notification** - Hướng dẫn lấy Server Key đúng

### Bạn cần làm:
1. 🔑 Lấy Server Key từ Firebase Console (bắt đầu bằng AAAA)
2. ✏️ Thay vào FCMNotificationSender.java dòng 41
3. 🔨 Build lại app
4. ✅ Test!

---

## 📞 Nếu Vẫn Không Được

### Gửi cho tôi:

1. **Screenshot Firebase Console → Cloud Messaging tab**
2. **Logcat khi gửi tin nhắn** (filter: FCMNotificationSender)
3. **Xác nhận:**
   - Server Key có bắt đầu bằng AAAA? 
   - Đã rebuild app?
   - FCM token có trong Firestore?

Chúc bạn thành công! 🎉

