# 🔔 Hướng Dẫn Gửi Notification Trực Tiếp Từ App (Không Cần Cloud Functions)

## 📋 Tổng Quan

Hệ thống đã được cấu hình để gửi notification **TRỰC TIẾP** từ app mà **KHÔNG CẦN Cloud Functions**. Đây là cách đơn giản hơn cho app nhỏ và vừa.

### ✅ Các File Đã Tạo

1. **FCMNotificationSender.java** - Utility class gửi notification qua FCM API
2. **NotificationManager.java** (đã cập nhật) - Quản lý việc gửi notification
3. **MessengerActivity.java** (đã cập nhật) - Tự động gửi notification khi gửi tin nhắn

---

## 🔑 Bước 1: Lấy Firebase Server Key (QUAN TRỌNG!)

### Cách 1: Từ Firebase Console (Legacy)

1. Mở Firebase Console: https://console.firebase.google.com/
2. Chọn project của bạn
3. Click vào ⚙️ **Settings** → **Project settings**
4. Chọn tab **Cloud Messaging**
5. Tìm section **Cloud Messaging API (Legacy)**
6. Copy **Server key** (bắt đầu bằng `AAAA...`)

⚠️ **Lưu ý:** Nếu không thấy Server Key:
- Click **"Enable Cloud Messaging API (Legacy)"** 
- Hoặc sử dụng Google Cloud Console (xem Cách 2)

### Cách 2: Từ Google Cloud Console (Khuyến nghị)

1. Vào Google Cloud Console: https://console.cloud.google.com/
2. Chọn project Firebase của bạn
3. Vào **APIs & Services** → **Credentials**
4. Tìm **API Keys** → Chọn key có tên "Browser key" hoặc "Server key"
5. Copy key

---

## 🛠️ Bước 2: Cấu Hình SERVER_KEY

### Mở file `FCMNotificationSender.java`

```java
// Dòng 20
private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE";
```

### Thay bằng Server Key của bạn:

```java
private static final String SERVER_KEY = "AAAAxxxxxxx:xxxxxxxxxxxxxxxxxxxxxxxxxxx";
```

**Ví dụ:**
```java
private static final String SERVER_KEY = "AAAAabcdefg:APA91bGHI1234567890jklmnopqrstuvwxyz";
```

---

## 📱 Bước 3: Thêm Permission Vào AndroidManifest.xml

Mở `app/src/main/AndroidManifest.xml` và đảm bảo có các permission:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Permission để gửi HTTP request -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Permission cho notification -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    
    <application>
        <!-- ...existing code... -->
        
        <!-- Firebase Messaging Service -->
        <service
            android:name=".services.FirebaseCloudMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        
    </application>
</manifest>
```

---

## 🎯 Cách Hoạt Động

### Flow Gửi Notification

```
1. User A gửi tin nhắn trong MessengerActivity
   ↓
2. Tin nhắn được lưu vào Firestore (qua ChatManager)
   ↓
3. MessengerActivity.onMessageSent() được gọi
   ↓
4. NotificationManager.sendMessageNotification() được gọi
   ↓
5. Lấy FCM token của User B từ Firestore
   ↓
6. FCMNotificationSender gửi HTTP POST đến FCM API
   ↓
7. FCM gửi notification đến thiết bị của User B
   ↓
8. FirebaseCloudMessagingService nhận và hiển thị notification
```

### Code Tự Động (Đã Implement)

Trong **MessengerActivity.java**, khi gửi tin nhắn thành công:

```java
@Override
public void onMessageSent() {
    // ...clear input...
    
    // GỬI NOTIFICATION TRỰC TIẾP
    NotificationManager.getInstance()
        .sendMessageNotification(
            partnerId,      // Người nhận
            senderName,     // Tên người gửi
            messageText,    // Nội dung
            coupleId,       // Couple ID
            currentUserId   // Người gửi ID
        );
}
```

---

## ✅ Kiểm Tra Hoạt Động

### 1. Build và chạy app

```cmd
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
gradlew assembleDebug
```

### 2. Test trên 2 thiết bị

**Thiết bị A:**
- Đăng nhập user A
- Gửi tin nhắn

**Thiết bị B:**
- Đăng nhập user B (partner của A)
- Đợi nhận notification

### 3. Kiểm tra logs

Trong **Logcat**, tìm:

```
✅ MessengerActivity: Notification request sent to {partnerId}
✅ NotificationManager: Notification sent successfully to {userId}
✅ FCMNotificationSender: Notification sent successfully
```

Nếu có lỗi:
```
❌ FCMNotificationSender: Server key not configured
❌ NotificationManager: Failed to send notification
```

---

## 🐛 Troubleshooting

### Lỗi: "Server key not configured"

**Nguyên nhân:** Chưa thay `YOUR_SERVER_KEY_HERE` trong `FCMNotificationSender.java`

**Giải pháp:** 
1. Lấy Server Key từ Firebase Console
2. Cập nhật `SERVER_KEY` trong code
3. Rebuild app

### Lỗi: "HTTP error code: 401"

**Nguyên nhân:** Server key không hợp lệ hoặc đã hết hạn

**Giải pháp:**
1. Kiểm tra lại Server Key
2. Tạo API key mới trong Google Cloud Console
3. Enable Cloud Messaging API (Legacy)

### Lỗi: "HTTP error code: 400"

**Nguyên nhân:** FCM token không hợp lệ hoặc đã hết hạn

**Giải pháp:**
1. Kiểm tra FCM token trong Firestore
2. Đăng xuất và đăng nhập lại để refresh token
3. Kiểm tra `FirebaseCloudMessagingService.onNewToken()`

### Không nhận được notification

**Kiểm tra:**

1. ✅ Server Key đã cấu hình đúng?
   ```java
   // Không được là:
   private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE";
   ```

2. ✅ FCM token đã được lưu trong Firestore?
   - Vào Firebase Console → Firestore
   - Collection: `users/{userId}`
   - Field: `fcmToken` phải có giá trị

3. ✅ Permission đã được grant?
   - Android 13+: Cần xin permission POST_NOTIFICATIONS
   - Kiểm tra Settings → Apps → Your App → Notifications

4. ✅ FirebaseCloudMessagingService đã đăng ký trong Manifest?
   ```xml
   <service android:name=".services.FirebaseCloudMessagingService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
   ```

5. ✅ App đang chạy background hay foreground?
   - Foreground: Notification hiển thị qua `onMessageReceived()`
   - Background: Android tự hiển thị notification

---

## 📊 So Sánh: Cloud Functions vs Direct Send

| Tiêu Chí | Cloud Functions | Direct Send (Hiện Tại) |
|----------|----------------|------------------------|
| **Setup** | Phức tạp hơn | ✅ Đơn giản |
| **Chi phí** | Có thể tốn phí | ✅ Miễn phí (trong giới hạn) |
| **Bảo mật** | ✅ An toàn hơn | Server key trong app |
| **Reliability** | ✅ Cao hơn | Phụ thuộc client |
| **Phù hợp cho** | App lớn, production | ✅ App nhỏ, prototype |

---

## 🔒 Lưu Ý Bảo Mật

### ⚠️ Server Key trong app là KHÔNG AN TOÀN

**Tại sao?**
- APK có thể bị decompile
- Hacker có thể lấy Server Key
- Có thể spam notification

**Giải pháp cho Production:**

### Option 1: Sử dụng Cloud Functions (Khuyến nghị)
```
App → Firestore → Cloud Function → FCM
```
- Server Key nằm trên server, an toàn
- Đã có sẵn code trong `functions/index.js`

### Option 2: ProGuard/R8 Obfuscation
```gradle
// app/build.gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Option 3: Build Backend API riêng
```
App → Your Backend → FCM
```

### Option 4: Sử dụng Firebase Admin SDK trên server

---

## 🚀 Deploy (Tuỳ Chọn)

### Nếu muốn chuyển sang Cloud Functions sau này:

```cmd
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
firebase deploy --only functions
```

Code Cloud Functions đã sẵn sàng trong `functions/index.js`

### Để tắt Direct Send và dùng Cloud Functions:

**1. Comment code trong MessengerActivity:**
```java
@Override
public void onMessageSent() {
    etMessage.setText("");
    btnSend.setEnabled(true);
    
    // COMMENT đoạn này để dùng Cloud Functions
    // NotificationManager.getInstance()
    //     .sendMessageNotification(...);
}
```

**2. Deploy Cloud Functions**
```cmd
firebase deploy --only functions:sendNotificationOnNewMessage
```

Cloud Function sẽ tự động phát hiện tin nhắn mới và gửi notification!

---

## 📝 Checklist Hoàn Thành

- [ ] Lấy Server Key từ Firebase Console
- [ ] Cập nhật `SERVER_KEY` trong `FCMNotificationSender.java`
- [ ] Kiểm tra permissions trong `AndroidManifest.xml`
- [ ] Build và cài app trên 2 thiết bị
- [ ] Test gửi tin nhắn
- [ ] Kiểm tra notification hiển thị
- [ ] Xem logs để debug (nếu cần)
- [ ] (Optional) Cấu hình ProGuard cho production

---

## 🎓 Tóm Tắt

### Ưu Điểm ✅
- Đơn giản, không cần setup Cloud Functions
- Không tốn phí Cloud Functions
- Notification gửi ngay lập tức
- Dễ debug và test

### Nhược Điểm ❌
- Server key nằm trong app (bảo mật thấp hơn)
- Phụ thuộc vào kết nối internet của người gửi
- Không có retry mechanism tự động

### Khi Nào Nên Chuyển Sang Cloud Functions?
- App có nhiều user (>1000)
- Cần bảo mật cao
- Cần reliability cao
- Cần analytics và monitoring

---

## 📞 Support

**Nếu gặp lỗi, kiểm tra:**
1. Logcat trong Android Studio
2. Firebase Console → Firestore (xem FCM tokens)
3. Google Cloud Console → APIs (enable Cloud Messaging API)

**Debug tips:**
```java
// Thêm log trong FCMNotificationSender.java
Log.d(TAG, "Sending to token: " + fcmToken);
Log.d(TAG, "Server key: " + SERVER_KEY.substring(0, 10) + "...");
Log.d(TAG, "Response code: " + responseCode);
```

**Chúc bạn thành công! 🎉**

