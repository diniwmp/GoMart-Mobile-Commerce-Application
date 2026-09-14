package lk.zenova.gomart.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import lk.zenova.gomart.helper.NotificationHelper;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM message received from: " + remoteMessage.getFrom());

        // Get current user ID
        com.google.firebase.auth.FirebaseAuth auth =
                com.google.firebase.auth.FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "";

        if (!remoteMessage.getData().isEmpty()) {
            String type    = remoteMessage.getData().get("type");
            String title   = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            String orderId = remoteMessage.getData().get("orderId");
            String status  = remoteMessage.getData().get("status");

            if (type == null) return;

            switch (type) {
                case "ORDER_PLACED":
                    NotificationHelper.notifyOrderPlaced(this,
                            userId,
                            orderId != null ? orderId : "");
                    break;

                case "PAYMENT_CONFIRMED":
                    NotificationHelper.notifyPaymentConfirmed(this,
                            orderId != null ? orderId : "", 0);
                    break;

                case "ORDER_STATUS":
                    NotificationHelper.notifyOrderStatus(this,
                            userId,
                            orderId != null ? orderId : "",
                            status != null ? status : "Updated");
                    break;


            }
        }


    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        saveFcmToken(token);
    }

    private void saveFcmToken(String token) {
        com.google.firebase.auth.FirebaseAuth auth =
                com.google.firebase.auth.FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "FCM token saved to Firestore"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to save token: " + e.getMessage()));
        }
    }
}