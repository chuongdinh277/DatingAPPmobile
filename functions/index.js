// Cloud Functions để gửi push notification
const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * ✅ Cloud Function TỰ ĐỘNG gửi notification khi có tin nhắn mới
 * Trigger: Realtime Database - chats/{coupleId}/{messageId}
 *
 * App đang dùng REALTIME DATABASE, không phải Firestore!
 * Đây là function ĐÚNG cho app của bạn
 */
exports.sendNotificationOnNewRealtimeMessage = functions.database
    .ref("/chats/{coupleId}/{messageId}")
    .onCreate(async (snapshot, context) => {
      const message = snapshot.val();
      const messageId = context.params.messageId;
      const coupleId = context.params.coupleId;

      console.log("📨 New message detected in Realtime Database");
      console.log("Couple ID:", coupleId);
      console.log("Message ID:", messageId);

      // Validate message data
      const senderId = message.senderId;
      const messageText = message.message || "New message";

      if (!coupleId || !senderId) {
        console.log("❌ Missing coupleId or senderId");
        return null;
      }

      try {
        // 1. Lấy thông tin couple từ Firestore để tìm người nhận
        const coupleDoc = await admin
            .firestore()
            .collection("couples")
            .doc(coupleId)
            .get();

        if (!coupleDoc.exists) {
          console.log("❌ Couple not found:", coupleId);
          return null;
        }

        const coupleData = coupleDoc.data();
        const user1Id = coupleData.user1Id;
        const user2Id = coupleData.user2Id;

        // 2. Xác định người nhận (người không gửi tin nhắn)
        const recipientId = senderId === user1Id ? user2Id : user1Id;

        console.log("👤 Sender:", senderId);
        console.log("👤 Recipient:", recipientId);

        // 3. Lấy FCM token của người nhận từ Firestore
        const recipientDoc = await admin
            .firestore()
            .collection("users")
            .doc(recipientId)
            .get();

        if (!recipientDoc.exists) {
          console.log("❌ Recipient not found:", recipientId);
          return null;
        }

        const recipientData = recipientDoc.data();
        const fcmToken = recipientData.fcmToken;

        if (!fcmToken) {
          console.log("⚠️ Recipient has no FCM token:", recipientId);
          return null;
        }

        console.log("🔑 FCM token found for recipient");

        // 4. Lấy tên người gửi
        const senderDoc = await admin
            .firestore()
            .collection("users")
            .doc(senderId)
            .get();

        const senderName = senderDoc.exists ?
            (senderDoc.data().name || "Someone") :
            "Someone";

        console.log("✉️ Sender name:", senderName);

        // 5. Tạo notification message với FCM API v1
        const notificationPayload = {
          notification: {
            title: senderName,
            body: truncateMessage(messageText, 100),
          },
          data: {
            coupleId: coupleId,
            senderId: senderId,
            senderName: senderName,
            messageText: messageText,
            messageId: messageId,
            type: "message",
            clickAction: "OPEN_MESSENGER",
          },
          token: fcmToken,
          android: {
            priority: "high",
            notification: {
              sound: "default",
              channelId: "couple_app_messages",
              icon: "ic_notification",
              color: "#FF6B9D",
              tag: coupleId,
            },
          },
          apns: {
            payload: {
              aps: {
                sound: "default",
                badge: 1,
              },
            },
          },
        };

        // 6. Gửi notification qua FCM (tự động dùng FCM API v1)
        const response = await admin.messaging().send(notificationPayload);
        console.log("✅ Notification sent successfully:", response);

        // 7. Lưu log notification (optional - để tracking)
        await admin.firestore().collection("notification_logs").add({
          messageId: messageId,
          coupleId: coupleId,
          recipientId: recipientId,
          senderId: senderId,
          senderName: senderName,
          fcmResponse: response,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          success: true,
        });

        return response;
      } catch (error) {
        console.error("❌ Error sending notification:", error);

        // Lưu log lỗi
        try {
          await admin.firestore().collection("notification_logs").add({
            messageId: messageId,
            coupleId: coupleId,
            senderId: senderId,
            error: error.message,
            errorStack: error.stack,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            success: false,
          });
        } catch (logError) {
          console.error("Error logging notification error:", logError);
        }

        return null;
      }
    });

/**
 * Helper function: Cắt ngắn message nếu quá dài
 * @param {string} message - Tin nhắn gốc
 * @param {number} maxLength - Độ dài tối đa
 * @return {string} - Tin nhắn đã được cắt ngắn nếu cần
 */
function truncateMessage(message, maxLength) {
  if (!message) return "";
  if (message.length <= maxLength) return message;
  return message.substring(0, maxLength - 3) + "...";
}

/**
 * Scheduled Function - Dọn dẹp notification logs cũ
 */
exports.cleanupOldNotificationLogs = functions.pubsub
    .schedule("0 2 * * *")
    .timeZone("Asia/Ho_Chi_Minh")
    .onRun(async (context) => {
      console.log("🧹 Starting cleanup of old notification logs");

      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      try {
        const logsRef = admin.firestore().collection("notification_logs");
        const snapshot = await logsRef
            .where("timestamp", "<", thirtyDaysAgo)
            .limit(500)
            .get();

        if (snapshot.empty) {
          console.log("No old notification logs to delete");
          return null;
        }

        const batch = admin.firestore().batch();
        snapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
        });

        await batch.commit();
        console.log(`✅ Deleted ${snapshot.size} old notification logs`);

        return {deleted: snapshot.size};
      } catch (error) {
        console.error("Error cleaning up notification logs:", error);
        return null;
      }
    });

/**
 * HTTP Endpoint - Test notification
 */
exports.testNotification = functions.https.onRequest(async (req, res) => {
  const {userId, title, body} = req.query;

  if (!userId) {
    res.status(400).send("Missing userId parameter");
    return;
  }

  try {
    const userDoc = await admin
        .firestore()
        .collection("users")
        .doc(userId)
        .get();

    if (!userDoc.exists) {
      res.status(404).send("User not found");
      return;
    }

    const fcmToken = userDoc.data().fcmToken;

    if (!fcmToken) {
      res.status(400).send("User has no FCM token");
      return;
    }

    const message = {
      notification: {
        title: title || "Test Notification",
        body: body || "This is a test notification",
      },
      token: fcmToken,
    };

    const response = await admin.messaging().send(message);

    res.json({
      success: true,
      messageId: response,
    });
  } catch (error) {
    console.error("Error sending test notification:", error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * HTTP Endpoint - Lấy thống kê notification
 */
exports.getNotificationStats = functions.https.onRequest(async (req, res) => {
  try {
    const logsRef = admin.firestore().collection("notification_logs");

    // Thống kê trong 24 giờ qua
    const oneDayAgo = new Date();
    oneDayAgo.setHours(oneDayAgo.getHours() - 24);

    const snapshot = await logsRef
        .where("timestamp", ">=", oneDayAgo)
        .get();

    let successCount = 0;
    let errorCount = 0;

    snapshot.forEach((doc) => {
      if (doc.data().success) {
        successCount++;
      } else {
        errorCount++;
      }
    });

    res.json({
      last24Hours: {
        total: snapshot.size,
        success: successCount,
        failed: errorCount,
        successRate: snapshot.size > 0 ?
          (successCount / snapshot.size * 100).toFixed(2) + "%" : "N/A",
      },
    });
  } catch (error) {
    console.error("Error getting notification stats:", error);
    res.status(500).json({
      error: error.message,
    });
  }
});
