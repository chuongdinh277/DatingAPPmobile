# Hướng dẫn Triển khai Push Notification với FCM HTTP v1 API

## 📌 Tổng quan

Cloud Messaging API (Legacy) đã bị deprecated và sẽ ngừng hoạt động vào **20/06/2024**. 

Giải pháp mới: **Sử dụng Firebase Cloud Functions** để gửi notification thông qua **FCM HTTP v1 API**.

### Cách hoạt động:

```
App (Client) → Firestore → Cloud Function → FCM HTTP v1 API → Device
```

1. **App lưu notification vào Firestore** (collection `notifications`)
2. **Cloud Function tự động trigger** khi có document mới
3. **Cloud Function gửi FCM notification** qua HTTP v1 API
4. **Device nhận notification** và hiển thị

---

## 🚀 Bước 1: Cài đặt Firebase CLI

```bash
# Cài đặt Firebase CLI (nếu chưa có)
npm install -g firebase-tools

# Đăng nhập vào Firebase
firebase login

# Khởi tạo Firebase Functions trong project
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
firebase init functions
```

**Chọn các option:**
- ✅ JavaScript
- ✅ ESLint (optional)
- ✅ Install dependencies with npm

---

## 🔧 Bước 2: Cấu hình Cloud Functions

### 2.1. File đã được tạo:

```
CoupleApp/
  functions/
    index.js         ← Cloud Function code
    package.json     ← Dependencies
```

### 2.2. Cài đặt dependencies:

```bash
cd functions
npm install
```

---

## 📦 Bước 3: Deploy Cloud Functions lên Firebase

```bash
# Deploy functions
firebase deploy --only functions

# Hoặc deploy tất cả
firebase deploy
```

### Kết quả mong đợi:

```
✔ functions[sendNotificationOnCreate]: Successful create operation.
✔ functions[cleanupOldNotifications]: Successful create operation.

✔ Deploy complete!
```

---

## 🧪 Bước 4: Test Push Notification

### 4.1. Kiểm tra Cloud Function đã deploy:

1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Chọn project của bạn
3. Vào **Functions** → Kiểm tra 2 functions:
   - `sendNotificationOnCreate` - Gửi notification
   - `cleanupOldNotifications` - Dọn dẹp notification cũ

### 4.2. Test trên app:

1. **Cài app trên 2 thiết bị**
2. **Đăng nhập 2 tài khoản** và ghép cặp
3. **Gửi tin nhắn từ thiết bị A**
4. **Kiểm tra notification trên thiết bị B**

### 4.3. Xem logs để debug:

```bash
# Xem logs real-time
firebase functions:log --only sendNotificationOnCreate

# Hoặc xem tất cả logs
firebase functions:log
```

---

## 🔍 Kiểm tra Firestore

Vào Firebase Console > Firestore Database > Collection `notifications`

Mỗi notification sẽ có cấu trúc:

```json
{
  "recipientUserId": "user123",
  "fcmToken": "fcm_token_here",
  "title": "John",
  "body": "Hello!",
  "coupleId": "couple123",
  "senderId": "sender123",
  "senderName": "John",
  "type": "message",
  "timestamp": "2024-01-15T10:30:00Z",
  "sent": true,
  "sentAt": "2024-01-15T10:30:01Z"
}
```

---

## ⚙️ Cấu hình nâng cao (Tùy chọn)

### Enable Firestore Indexes (nếu cần):

```bash
firebase deploy --only firestore:indexes
```

### Tạo file `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "notifications",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "recipientUserId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    }
  ]
}
```

---

## 💰 Chi phí Cloud Functions

### Spark Plan (Free):
- ✅ **2,000,000 invocations/month** - Đủ cho app nhỏ
- ✅ **400,000 GB-seconds/month**
- ✅ **200,000 CPU-seconds/month**

### Blaze Plan (Pay as you go):
- Sau khi vượt quá Spark plan
- Khoảng **$0.40 per million invocations**

**Lưu ý:** App bạn có thể dùng Free tier vì Cloud Function chỉ trigger khi có tin nhắn mới.

---

## 🐛 Troubleshooting

### Lỗi 1: "Permission denied" khi deploy

**Giải pháp:**
```bash
firebase login --reauth
firebase use --add
```

### Lỗi 2: Functions không trigger

**Kiểm tra:**
1. Cloud Function đã được deploy thành công chưa?
2. Firestore collection name đúng chưa? (`notifications`)
3. Xem logs: `firebase functions:log`

### Lỗi 3: Notification không gửi được

**Kiểm tra:**
1. FCM token có hợp lệ không?
2. Device có bật notification permission chưa?
3. Xem error trong Firestore document (field `error`)

---

## 📊 Monitor Cloud Functions

### 1. Firebase Console:
- **Functions** > **Dashboard** → Xem số lần invoke, errors, execution time

### 2. Cloud Functions logs:
```bash
# Real-time logs
firebase functions:log --only sendNotificationOnCreate

# Filter by error
firebase functions:log --only sendNotificationOnCreate --lines 50 | findstr "error"
```

---

## 🔐 Bảo mật

### Ưu điểm của Cloud Functions:

✅ **Không cần Server Key trên client** - An toàn hơn  
✅ **Sử dụng Service Account** - Google tự động quản lý  
✅ **Không lộ credentials** - Server Key không bao giờ xuất hiện trong app  
✅ **FCM HTTP v1 API** - API mới nhất, được Google recommend  

---

## 📝 Tóm tắt các thay đổi

### ✅ Code đã cập nhật:

1. **NotificationManager.java** - Lưu notification vào Firestore
2. **Cloud Function** - Tự động gửi FCM notification
3. **package.json** - Dependencies cho Cloud Functions

### ❌ Code đã xóa:

1. **Server Key** - Không còn cần nữa
2. **HTTP request trực tiếp** - Cloud Function xử lý

### 🎯 Lợi ích:

- ✅ Tuân thủ FCM HTTP v1 API mới
- ✅ Bảo mật tốt hơn
- ✅ Dễ maintain và scale
- ✅ Không lo deprecated API

---

## 🚀 Next Steps

1. ✅ **Deploy Cloud Functions** (quan trọng nhất)
2. ✅ **Test notification** trên 2 thiết bị
3. ✅ **Monitor logs** để đảm bảo hoạt động tốt
4. ⏳ **Enable Blaze Plan** nếu cần (optional, sau khi app lớn)

---

## 📚 Tài liệu tham khảo

- [Firebase Cloud Functions Documentation](https://firebase.google.com/docs/functions)
- [FCM HTTP v1 API Reference](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
- [Migrate from Legacy to HTTP v1](https://firebase.google.com/docs/cloud-messaging/migrate-v1)

