package lk.zenova.gomart.helper;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import lk.zenova.gomart.R;
import lk.zenova.gomart.activity.MainActivity;
import lk.zenova.gomart.model.Notification;

public class GoMartFirebaseMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title   = "GoMart";
        String message = "";
        String orderId = "";

        if (remoteMessage.getNotification() != null) {
            title   = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
        }

        if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().get("title") != null)
                title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().get("message") != null)
                message = remoteMessage.getData().get("message");
            if (remoteMessage.getData().get("orderId") != null)
                orderId = remoteMessage.getData().get("orderId");
        }

        showPhoneNotification(title, message, orderId);

        saveToFirestoreInbox(title, message,
                remoteMessage.getData().get("type"),
                orderId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveTokenToFirestore(token);
    }

    private void showPhoneNotification(String title,
                                       String message,
                                       String orderId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (orderId != null && !orderId.isEmpty()) {
            intent.putExtra("orderId", orderId);
            intent.putExtra("openNotifications", true);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        android.graphics.Bitmap largeBitmap =
                android.graphics.BitmapFactory.decodeResource(
                        this.getResources(),
                        R.drawable.splash_app_icon);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this, NotificationHelper.CHANNEL_ORDERS)
                        .setSmallIcon(
                                R.drawable.ic_notification)
                        .setLargeIcon(largeBitmap)
                        .setColor(android.graphics.Color
                                .parseColor("#FF6600"))
                        .setColorized(true)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message))
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE);
        manager.notify(
                (int) System.currentTimeMillis(),
                builder.build());
    }

    private void saveToFirestoreInbox(String title,
                                      String message,
                                      String type,
                                      String orderId) {
        com.google.firebase.auth.FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Notification notif = new Notification(
                title, message,
                type != null ? type : "ORDER",
                orderId);

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(user.getUid())
                .collection("items")
                .add(notif);
    }

    // Save FCM token
    private void saveTokenToFirestore(String token) {
        com.google.firebase.auth.FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("fcmToken", token);
    }
}