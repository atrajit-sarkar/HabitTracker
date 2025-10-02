// Firebase Cloud Functions for sending chat notifications
// Deploy this to Firebase to enable automatic push notifications

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Cloud Function: Send FCM notification when a new chat message is created
 * Trigger: Firestore onCreate event on 'chats/{chatId}/messages/{messageId}'
 */
exports.sendChatNotification = functions.firestore
    .document('chats/{chatId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        try {
            const message = snap.data();
            const chatId = context.params.chatId;
            
            console.log('New message in chat:', chatId);
            console.log('Message data:', message);
            
            // Get chat details to find all participants
            const chatDoc = await admin.firestore()
                .collection('chats')
                .doc(chatId)
                .get();
            
            if (!chatDoc.exists) {
                console.error('Chat not found:', chatId);
                return null;
            }
            
            const chat = chatDoc.data();
            const participants = chat.participants || [];
            
            // Send notification to all participants except the sender
            const notificationPromises = participants
                .filter(participantId => participantId !== message.senderId)
                .map(async (recipientId) => {
                    try {
                        // Get recipient's FCM token from users collection
                        const userDoc = await admin.firestore()
                            .collection('users')
                            .doc(recipientId)
                            .get();
                        
                        if (!userDoc.exists) {
                            console.log('User not found:', recipientId);
                            return null;
                        }
                        
                        const userData = userDoc.data();
                        const fcmToken = userData.fcmToken;
                        
                        if (!fcmToken) {
                            console.log('No FCM token for user:', recipientId);
                            return null;
                        }
                        
                        // Prepare notification payload
                        const notificationPayload = {
                            token: fcmToken,
                            data: {
                                friendId: message.senderId,
                                friendName: message.senderName || 'Friend',
                                friendAvatar: message.senderAvatar || '😊',
                                friendPhotoUrl: message.senderPhotoUrl || '',
                                messageContent: message.content || '',
                                messageType: message.type || 'TEXT'
                            },
                            android: {
                                priority: 'high',
                                ttl: 86400 // 24 hours
                            }
                        };
                        
                        console.log('Sending notification to:', recipientId);
                        
                        // Send the notification
                        const response = await admin.messaging().send(notificationPayload);
                        
                        console.log('Successfully sent notification:', response);
                        return response;
                        
                    } catch (error) {
                        console.error('Error sending notification to user:', recipientId, error);
                        return null;
                    }
                });
            
            // Wait for all notifications to be sent
            const results = await Promise.all(notificationPromises);
            console.log('Notification sending complete. Results:', results);
            
            return results;
            
        } catch (error) {
            console.error('Error in sendChatNotification function:', error);
            return null;
        }
    });

/**
 * Optional: Clean up old FCM tokens when they fail
 */
exports.cleanupInvalidTokens = functions.https.onCall(async (data, context) => {
    // This can be called from the app when a notification fails
    const { userId } = data;
    
    if (!userId) {
        throw new functions.https.HttpsError('invalid-argument', 'userId is required');
    }
    
    try {
        await admin.firestore()
            .collection('users')
            .doc(userId)
            .update({
                fcmToken: admin.firestore.FieldValue.delete()
            });
        
        return { success: true, message: 'Token cleaned up' };
    } catch (error) {
        throw new functions.https.HttpsError('internal', error.message);
    }
});
