# Hướng dẫn cấu hình Push Notification cho CoupleApp

## 1. Lấy Firebase Server Key

Để gửi push notification, bạn cần lấy **Firebase Server Key** từ Firebase Console:

### Các bước:

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Chọn project của bạn (CoupleApp)
3. Click vào **Settings** (biểu tượng ⚙️) > **Project settings**
4. Chọn tab **Cloud Messaging**
5. Tìm phần **Cloud Messaging API (Legacy)** 
   - Nếu API chưa được bật, click **Enable Cloud Messaging API (Legacy)**
6. Copy **Server key** 

### Cập nhật Server Key vào code:

Mở file: `app/src/main/java/com/example/couple_app/managers/NotificationManager.java`

Tìm dòng:
```java
private static final String SERVER_KEY = "YOUR_FIREBASE_SERVER_KEY_HERE";
```

Thay thế `YOUR_FIREBASE_SERVER_KEY_HERE` bằng Server Key bạn vừa copy.

Ví dụ:
```java
private static final String SERVER_KEY = "AAAAxxxxxx:APA91bF...";
```

## 2. Kiểm tra Permissions trong AndroidManifest.xml

Đảm bảo các permissions sau đã được thêm (đã có sẵn):
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 3. Kiểm tra Firebase Cloud Messaging Service

Service đã được đăng ký trong AndroidManifest.xml:
```xml
<service
    android:name=".services.FirebaseCloudMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## 4. Test Push Notification

### Cách test:

1. **Build và cài đặt app** trên 2 thiết bị (hoặc 1 thiết bị thật + 1 emulator)
2. **Đăng nhập 2 tài khoản khác nhau** và ghép cặp với nhau
3. **Gửi tin nhắn** từ thiết bị A sang thiết bị B
4. **Kiểm tra notification** xuất hiện trên thiết bị B

### Xem logs để debug:

```bash
adb logcat | grep -E "FCMService|NotificationManager|MessengerActivity"
```

Các log quan trọng:
- `FCM Token: [token]` - Token đã được lấy thành công
- `FCM token saved successfully` - Token đã được lưu vào Firestore
- `Message sent successfully` - Tin nhắn đã được gửi
- `Notification sent successfully` - Thông báo đã được gửi qua FCM

## 5. Lưu ý quan trọng

### A. Notification khi app đang mở:
- Khi MessengerActivity đang mở, thông báo vẫn sẽ được gửi
- Bạn có thể tùy chỉnh để không hiển thị notification khi đang ở trong chat

### B. Notification khi app ở background:
- Notification sẽ xuất hiện ở status bar
- Click vào notification sẽ mở MessengerActivity với thông tin đối phương

### C. Firebase Cloud Messaging API (Legacy) vs FCM HTTP v1:
- Code hiện tại sử dụng **Legacy API** (đơn giản hơn)
- Google khuyến nghị chuyển sang **FCM HTTP v1 API** trong tương lai
- Legacy API vẫn hoạt động tốt và đủ cho app này

## 6. Các vấn đề thường gặp

### Vấn đề 1: Không nhận được notification
**Nguyên nhân:**
- Server Key chưa được cấu hình
- Token chưa được lưu vào Firestore
- Thiết bị không có kết nối internet

**Giải pháp:**
- Kiểm tra Server Key đã đúng chưa
- Kiểm tra log xem token đã được lưu chưa
- Kiểm tra kết nối mạng

### Vấn đề 2: App crash khi gửi tin nhắn
**Nguyên nhân:**
- Icon notification không tồn tại
- Permission chưa được cấp

**Giải pháp:**
- Đảm bảo file `ic_notification.xml` đã được tạo
- Yêu cầu permission `POST_NOTIFICATIONS` trên Android 13+

### Vấn đề 3: Notification không có sound/vibration
**Nguyên nhân:**
- Channel notification chưa được cấu hình đúng
- Settings notification của app bị tắt

**Giải pháp:**
- Kiểm tra code tạo NotificationChannel
- Kiểm tra Settings > Apps > CoupleApp > Notifications

## 7. Cải tiến trong tương lai

1. **Thêm notification cho các sự kiện khác:**
   - Kỷ niệm quan trọng
   - Lời nhắc nhở lịch hẹn
   - Tin nhắn tự động từ AI

2. **Tùy chỉnh notification:**
   - Notification với hình ảnh
   - Reply trực tiếp từ notification
   - Notification group

3. **Analytics:**
   - Theo dõi số lượng notification được gửi
   - Tỷ lệ người dùng click vào notification

## 8. Bảo mật

⚠️ **QUAN TRỌNG:**
- **KHÔNG** commit Server Key lên Git
- Nên sử dụng **Environment Variables** hoặc **local.properties** để lưu Server Key
- Xem xét chuyển sang **FCM HTTP v1 API** với OAuth 2.0 để bảo mật tốt hơn

## 9. Tài liệu tham khảo

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Guide](https://developer.android.com/develop/ui/views/notifications)
- [FCM Server Reference](https://firebase.google.com/docs/cloud-messaging/server)

