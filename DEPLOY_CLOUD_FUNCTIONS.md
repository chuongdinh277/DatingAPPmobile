# 🚀 Hướng Dẫn Deploy Cloud Functions (Giải Pháp Cho Legacy API Bị Tắt)

## ⚠️ VẤN ĐỀ

Firebase đã **TẮT Cloud Messaging API (Legacy)** từ 6/20/2024:
```
Cloud Messaging API (Legacy) - Disabled
```

**Điều này có nghĩa là:**
- ❌ KHÔNG CÒN Server Key (bắt đầu bằng AAAA)
- ❌ KHÔNG THỂ gửi notification trực tiếp từ app
- ✅ PHẢI DÙNG Cloud Functions với FCM API v1

---

## ✅ GIẢI PHÁP: Sử Dụng Cloud Functions

Cloud Functions sẽ:
1. Tự động phát hiện tin nhắn mới trong Realtime Database
2. Tự động gửi notification qua FCM API v1 (mới nhất)
3. An toàn hơn (không cần Server Key trong app)
4. Miễn phí cho app nhỏ (Spark Plan)

---

## 📋 Các Bước Deploy

### Bước 1: Cài Firebase CLI (nếu chưa có)

Mở **Command Prompt** với quyền Admin:

```cmd
npm install -g firebase-tools
```

Nếu không có npm, download Node.js trước:
https://nodejs.org/

### Bước 2: Đăng nhập Firebase

```cmd
firebase login
```

Trình duyệt sẽ mở → Đăng nhập tài khoản Google của bạn.

### Bước 3: Kiểm tra project hiện tại

```cmd
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
firebase projects:list
```

Đảm bảo project "couples-app-b83be" có trong danh sách.

### Bước 4: Chọn project (nếu chưa)

```cmd
firebase use couples-app-b83be
```

### Bước 5: Cài dependencies cho functions

```cmd
cd functions
npm install
```

### Bước 6: Deploy Cloud Functions

```cmd
firebase deploy --only functions
```

**Chờ khoảng 2-3 phút...**

Kết quả thành công sẽ hiện:
```
✔  Deploy complete!

Functions:
  sendNotificationOnNewRealtimeMessage(us-central1)
  cleanupOldNotificationLogs(us-central1)
  testNotification(us-central1)
  getNotificationStats(us-central1)
```

---

## 🎯 Cách Hoạt Động

### Flow Tự Động:

```
User A gửi tin nhắn
    ↓
Tin nhắn lưu vào Realtime Database
    path: /chats/{coupleId}/{messageId}
    ↓
Cloud Function "sendNotificationOnNewRealtimeMessage" được trigger
    ↓
Function lấy thông tin:
    - Couple data từ Firestore
    - FCM token của User B
    - Tên của User A
    ↓
Gửi notification qua FCM API v1 (tự động)
    ↓
User B nhận notification
```

**Bạn KHÔNG CẦN thay đổi code Android!** Mọi thứ tự động.

---

## 🧪 Kiểm Tra Sau Khi Deploy

### 1. Kiểm tra Functions đã deploy

```cmd
firebase functions:list
```

Phải thấy:
```
sendNotificationOnNewRealtimeMessage
```

### 2. Test gửi tin nhắn

Trên app:
1. User A gửi tin nhắn cho User B
2. Đợi 2-3 giây
3. User B sẽ nhận notification

### 3. Xem logs của Cloud Function

```cmd
firebase functions:log
```

Hoặc xem realtime:
```cmd
firebase functions:log --only sendNotificationOnNewRealtimeMessage
```

**Logs thành công sẽ hiện:**
```
📨 New message detected in Realtime Database
👤 Sender: xxx
👤 Recipient: yyy
🔑 FCM token found for recipient
✉️ Sender name: John
✅ Notification sent successfully: projects/...
```

**Nếu có lỗi:**
```
❌ Missing coupleId or senderId
❌ Couple not found
⚠️ Recipient has no FCM token
```

---

## 📊 Chi Phí

### Spark Plan (Miễn Phí)
- ✅ 125,000 function invocations/month
- ✅ 40,000 GB-seconds compute/month
- ✅ Đủ cho ~4,000 tin nhắn/ngày

**Với app nhỏ → HOÀN TOÀN MIỄN PHÍ!**

### Nếu cần nhiều hơn → Upgrade Blaze Plan
- Chỉ trả tiền phần vượt quá
- ~$0.40 per million invocations (rất rẻ)

---

## 🐛 Troubleshooting

### Lỗi: "Permission denied"

**Giải pháp:**
```cmd
firebase login --reauth
```

### Lỗi: "Project not found"

**Kiểm tra:**
```cmd
firebase projects:list
```

**Chọn lại project:**
```cmd
firebase use couples-app-b83be
```

### Lỗi khi deploy: "npm not found"

**Cài Node.js:**
https://nodejs.org/

Sau đó chạy lại:
```cmd
npm install -g firebase-tools
```

### Notification không gửi được

**Kiểm tra logs:**
```cmd
firebase functions:log --only sendNotificationOnNewRealtimeMessage
```

**Checklist:**
- [ ] Cloud Function đã deploy thành công?
- [ ] FCM token đã lưu trong Firestore (collection: users)?
- [ ] Couple data có trong Firestore (collection: couples)?
- [ ] Tin nhắn đã lưu vào Realtime Database (/chats/{coupleId}/...)?

**Debug bằng Firebase Console:**
1. Vào https://console.firebase.google.com/
2. Chọn project → Functions
3. Click vào "sendNotificationOnNewRealtimeMessage"
4. Xem Logs tab

---

## 🎓 So Sánh: Legacy API vs Cloud Functions

| | Legacy API (Cũ) | Cloud Functions (Mới) |
|---|---|---|
| **Status** | ❌ Đã TẮT | ✅ Đang hoạt động |
| **Server Key** | Cần | ❌ Không cần |
| **Bảo mật** | Thấp (key trong app) | ✅ Cao (server-side) |
| **Setup** | Đơn giản | Phức tạp hơn |
| **Chi phí** | Miễn phí | ✅ Miễn phí (app nhỏ) |
| **API** | HTTP Legacy | ✅ FCM v1 (mới nhất) |
| **Khuyến nghị** | ❌ Không dùng | ✅ DÙNG CÁI NÀY |

---

## 📝 Checklist Hoàn Thành

- [ ] Cài Firebase CLI: `npm install -g firebase-tools`
- [ ] Đăng nhập: `firebase login`
- [ ] Chọn project: `firebase use couples-app-b83be`
- [ ] Cài dependencies: `cd functions && npm install`
- [ ] Deploy: `firebase deploy --only functions`
- [ ] Xem logs: `firebase functions:log`
- [ ] Test gửi tin nhắn
- [ ] Kiểm tra User B nhận notification

---

## 🔍 Kiểm Tra FCM Token Trong Firestore

### Cách 1: Firebase Console
1. Vào https://console.firebase.google.com/
2. Chọn project → Firestore Database
3. Collection: `users`
4. Click vào user document
5. Kiểm tra field `fcmToken` có giá trị

### Cách 2: Test notification thủ công

Sau khi deploy, gọi test function:
```
https://us-central1-couples-app-b83be.cloudfunctions.net/testNotification?userId=USER_ID&title=Test&body=Hello
```

Thay `USER_ID` bằng ID thực của user.

---

## 📞 Các Lệnh Hữu Ích

### Xem danh sách functions
```cmd
firebase functions:list
```

### Xem logs realtime
```cmd
firebase functions:log --only sendNotificationOnNewRealtimeMessage
```

### Xem thống kê
```
https://us-central1-couples-app-b83be.cloudfunctions.net/getNotificationStats
```

### Undeploy function (nếu cần)
```cmd
firebase functions:delete sendNotificationOnNewRealtimeMessage
```

### Chỉ deploy 1 function cụ thể
```cmd
firebase deploy --only functions:sendNotificationOnNewRealtimeMessage
```

---

## 🎉 Kết Luận

### Tóm tắt những gì đã làm:

1. ✅ **Đã sửa lỗi duplicate tin nhắn** - Thêm check messageId
2. ✅ **Tạo Cloud Function mới** - Lắng nghe Realtime Database (không phải Firestore)
3. ✅ **Sử dụng FCM API v1** - Firebase Admin SDK tự động dùng API mới
4. ✅ **Xóa code gửi notification từ app** - Để Cloud Function xử lý
5. ✅ **An toàn hơn** - Không cần Server Key trong app

### Bạn cần làm DUY NHẤT:

```cmd
cd C:\Users\Anhnguyen\AndroidStudioProjects\CoupleApp
firebase login
firebase use couples-app-b83be
cd functions
npm install
cd..
firebase deploy --only functions
```

**Xong!** Notification sẽ tự động hoạt động! 🎉

---

## 💡 Tips

1. **Logs rất quan trọng** - Luôn check logs khi có vấn đề:
   ```cmd
   firebase functions:log
   ```

2. **Test trên thiết bị thật** - Emulator không nhận notification tốt

3. **Đảm bảo FCM token được lưu** - Kiểm tra Firestore

4. **Be patient** - Cloud Function cần 1-2 giây để xử lý

5. **Monitor usage** - Xem Firebase Console → Functions → Usage để đảm bảo không vượt quota

---

Chúc bạn thành công! Nếu gặp lỗi, paste logs vào đây để mình hỗ trợ! 🚀

