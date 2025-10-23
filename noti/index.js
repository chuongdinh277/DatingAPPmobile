const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
require('dotenv').config();

const app = express();

// Cấu hình CORS
app.use(cors());
app.use(express.json());

// Khởi tạo Firebase Admin SDK
let firebaseInitialized = false;

try {
  // Cách 1: Dùng Service Account từ environment variable
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL || "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app"
    });
    firebaseInitialized = true;
    console.log('✅ Firebase initialized from FIREBASE_SERVICE_ACCOUNT env var');
  }
  // Cách 2: Dùng các trường riêng lẻ
  else if (process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID || "couples-app-b83be",
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
      }),
      databaseURL: process.env.FIREBASE_DATABASE_URL || "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app"
    });
    firebaseInitialized = true;
    console.log('✅ Firebase initialized from individual env vars');
  }
  // Cách 3: Dùng file local (chỉ cho development)
  else {
    try {
      const serviceAccount = require('./service-account.json');
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: "https://couples-app-b83be-default-rtdb.asia-southeast1.firebasedatabase.app"
      });
      firebaseInitialized = true;
      console.log('✅ Firebase initialized from service-account.json');
    } catch (fileError) {
      console.log('⚠️ service-account.json not found');
    }
  }
} catch (error) {
  console.error('❌ Firebase initialization error:', error.message);
}

const db = firebaseInitialized ? admin.firestore() : null;

// API endpoints
app.get('/', (req, res) => {
  res.json({
    status: 'Server is running',
    message: 'Couple App Notification Server',
    timestamp: new Date().toISOString(),
    firebaseInitialized
  });
});

app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    firebaseInitialized,
    timestamp: new Date().toISOString()
  });
});

/**
 * POST /api/send-notification
 * Gửi FCM notification đến một user
 *
 * Body:
 * {
 *   "toUserId": "user_id",
 *   "title": "Message title",
 *   "body": "Message body",
 *   "data": {
 *     "coupleId": "couple_id",
 *     "fromUserId": "sender_id",
 *     "fromUsername": "sender_name"
 *   }
 * }
 */
app.post('/api/send-notification', async (req, res) => {
  try {
    if (!firebaseInitialized) {
      return res.status(500).json({
        success: false,
        error: 'Firebase not initialized. Please configure environment variables.'
      });
    }

    const { toUserId, title, body, data } = req.body;

    // ✅ Validation chi tiết hơn
    if (!toUserId || typeof toUserId !== 'string' || toUserId.trim() === '') {
      return res.status(400).json({
        success: false,
        error: 'Invalid toUserId'
      });
    }

    if (!title || typeof title !== 'string' || title.trim() === '') {
      return res.status(400).json({
        success: false,
        error: 'Invalid title'
      });
    }

    if (!body || typeof body !== 'string' || body.trim() === '') {
      return res.status(400).json({
        success: false,
        error: 'Invalid body'
      });
    }

    console.log(`📩 Processing notification for user: ${toUserId}`);

    // Lấy FCM token từ Firestore
    const userDoc = await db.collection('users').doc(toUserId).get();

    if (!userDoc.exists) {
      console.log(`❌ User not found: ${toUserId}`);
      return res.status(404).json({
        success: false,
        error: 'User not found'
      });
    }

    const userData = userDoc.data();
    const fcmToken = userData.fcmToken;

    if (!fcmToken || fcmToken.trim() === '') {
      console.log(`⚠️ User ${toUserId} does not have FCM token`);
      return res.status(400).json({
        success: false,
        error: 'User does not have FCM token',
        hint: 'User needs to login on the app to register FCM token'
      });
    }

    // ✅ Chuẩn bị data - đảm bảo tất cả value là string
    const notificationData = {};
    if (data && typeof data === 'object') {
      Object.keys(data).forEach(key => {
        const value = data[key];
        // Chuyển tất cả value thành string (FCM yêu cầu)
        notificationData[key] = value != null ? String(value) : '';
      });
    }

    // Thêm các field bắt buộc
    notificationData.timestamp = new Date().toISOString();
    notificationData.type = notificationData.type || 'message';
    notificationData.clickAction = 'OPEN_MESSENGER';

    // ✅ Gửi notification với cấu trúc đúng
    const message = {
      token: fcmToken,
      notification: {
        title: title.substring(0, 100), // Giới hạn độ dài
        body: body.substring(0, 200)
      },
      data: notificationData,
      android: {
        priority: 'high',
        notification: {
          channelId: 'couple_app_messages',
          sound: 'default',
          color: '#FF6B9D',
          priority: 'high',
          defaultSound: true,
          defaultVibrateTimings: true
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1
          }
        }
      }
    };

    console.log(`📤 Sending notification to token: ${fcmToken.substring(0, 20)}...`);

    // ✅ Gửi với error handling tốt hơn
    let response;
    try {
      response = await admin.messaging().send(message);
      console.log('✅ Notification sent successfully:', response);
    } catch (sendError) {
      console.error('❌ FCM send error:', sendError);

      // Xử lý các loại lỗi cụ thể
      if (sendError.code === 'messaging/invalid-registration-token' ||
          sendError.code === 'messaging/registration-token-not-registered') {
        // Token không hợp lệ - xóa khỏi DB
        try {
          await db.collection('users').doc(toUserId).update({
            fcmToken: admin.firestore.FieldValue.delete()
          });
          console.log(`🗑️ Removed invalid FCM token for user: ${toUserId}`);
        } catch (updateError) {
          console.error('Error removing invalid token:', updateError);
        }

        return res.status(400).json({
          success: false,
          error: 'Invalid or expired FCM token',
          code: sendError.code
        });
      }

      throw sendError;
    }

    res.json({
      success: true,
      messageId: response,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    console.error('❌ Error sending notification:', error);
    res.status(500).json({
      success: false,
      error: error.message,
      code: error.code || 'UNKNOWN_ERROR'
    });
  }
});

/**
 * POST /api/send-notification-by-token
 * Gửi notification trực tiếp đến FCM token
 */
app.post('/api/send-notification-by-token', async (req, res) => {
  try {
    if (!firebaseInitialized) {
      return res.status(500).json({
        success: false,
        error: 'Firebase not initialized'
      });
    }

    const { fcmToken, title, body, data } = req.body;

    if (!fcmToken || !title || !body) {
      return res.status(400).json({
        success: false,
        error: 'Missing required fields: fcmToken, title, body'
      });
    }

    // ✅ Chuẩn bị data
    const notificationData = {};
    if (data && typeof data === 'object') {
      Object.keys(data).forEach(key => {
        notificationData[key] = data[key] != null ? String(data[key]) : '';
      });
    }
    notificationData.timestamp = new Date().toISOString();

    const message = {
      token: fcmToken,
      notification: {
        title: title.substring(0, 100),
        body: body.substring(0, 200)
      },
      data: notificationData,
      android: {
        priority: 'high',
        notification: {
          channelId: 'couple_app_messages',
          sound: 'default',
          color: '#FF6B9D',
          priority: 'high'
        }
      }
    };

    const response = await admin.messaging().send(message);

    res.json({
      success: true,
      messageId: response
    });

  } catch (error) {
    console.error('❌ Error:', error);
    res.status(500).json({
      success: false,
      error: error.message,
      code: error.code || 'UNKNOWN_ERROR'
    });
  }
});

const PORT = process.env.PORT || 3000;

// Chỉ start server khi chạy local (không phải trên Vercel)
if (process.env.NODE_ENV !== 'production' && !process.env.VERCEL) {
  app.listen(PORT, () => {
    console.log(`\n🚀 Server is running on port ${PORT}`);
    console.log(`📱 Notification API ready`);
    console.log(`🔔 Firebase initialized: ${firebaseInitialized}\n`);
  });
}

// Export cho Vercel
module.exports = app;
