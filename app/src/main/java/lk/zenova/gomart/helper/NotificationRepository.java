package lk.zenova.gomart.helper;

import com.google.firebase.firestore.FirebaseFirestore;

import lk.zenova.gomart.model.Notification;

public class NotificationRepository {

    private final FirebaseFirestore db;

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }


    public void saveToUserInbox(String userId,
                                Notification notification) {
        db.collection("notifications")
                .document(userId)
                .collection("items")
                .add(notification);
    }

    public void saveOrderNotification(String userId,
                                      String title,
                                      String message,
                                      String orderId) {
        Notification notif = new Notification(
                title, message, "ORDER", orderId);
        saveToUserInbox(userId, notif);
    }

    public void markAsRead(String userId, String notifId) {
        db.collection("notifications")
                .document(userId)
                .collection("items")
                .document(notifId)
                .update("isRead", true);
    }

    public void markAllAsRead(String userId) {
        db.collection("notifications")
                .document(userId)
                .collection("items")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (var doc : snapshots.getDocuments()) {
                        doc.getReference().update("isRead", true);
                    }
                });
    }
}