package lk.zenova.gomart.helper;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import lk.zenova.gomart.R;
import lk.zenova.gomart.activity.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_ORDERS = "gomart_orders";
    public static final String CHANNEL_PROMO = "gomart_promo";
    public static final String CHANNEL_GENERAL = "gomart_channel";

    public static final int NOTIF_ORDER_PLACED = 1001;
    public static final int NOTIF_ORDER_STATUS = 1002;
    public static final int NOTIF_PAYMENT = 1003;
    public static final int NOTIF_PROMO = 1004;

    public static final int NOTIF_WELCOME = 1005;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            NotificationChannel ordersChannel = new NotificationChannel(
                    CHANNEL_ORDERS,
                    "Order Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ordersChannel.setDescription("Notifications for order updates");
            ordersChannel.enableVibration(true);
            manager.createNotificationChannel(ordersChannel);

            NotificationChannel promoChannel = new NotificationChannel(
                    CHANNEL_PROMO,
                    "Promotions & Offers",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            promoChannel.setDescription("Promotional offers and deals");
            manager.createNotificationChannel(promoChannel);

            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            manager.createNotificationChannel(generalChannel);
        }
    }

    public static void showNotification(Context context, int notifId,
                                        String channelId, String title,
                                        String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        android.graphics.Bitmap largeBitmap =
                android.graphics.BitmapFactory.decodeResource(
                        context.getResources(),
                        R.drawable.splash_app_icon);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeBitmap)
                .setColor(android.graphics.Color.parseColor("#FF6600"))
                .setColorized(true)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notifManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notifManager.notify(notifId, builder.build());
    }

    public static void notifyOrderPlaced(Context context, String orderId) {
        AppPreferences prefs = new AppPreferences(context);
        if (!prefs.isOrderNotifEnabled()) return;
        showNotification(
                context,
                NOTIF_ORDER_PLACED,
                CHANNEL_ORDERS,
                "Order Placed Successfully!",
                "Your order #" + orderId + " has been placed. We are processing it now."
        );
    }

    public static void notifyPaymentConfirmed(Context context, String orderId, double amount) {
        AppPreferences prefs = new AppPreferences(context);
        if (!prefs.isOrderNotifEnabled()) return;
        showNotification(
                context,
                NOTIF_PAYMENT,
                CHANNEL_ORDERS,
                "Payment Confirmed!",
                "Payment of LKR " + String.format("%.2f", amount) +
                        " confirmed for order #" + orderId + ". Thank you!"
        );
    }

    public static void notifyOrderPlaced(Context context,
                                         String userId,
                                         String orderId) {
        AppPreferences prefs = new AppPreferences(context);
        if (!prefs.isOrderNotifEnabled()) return;

        String title   = "Order Placed Successfully!";
        String message = "Your order #" + orderId + " has been confirmed.";

        showNotification(context, NOTIF_ORDER_PLACED,
                CHANNEL_ORDERS, title, message);

        new NotificationRepository()
                .saveOrderNotification(userId, title, message, orderId);
    }

    public static void notifyOrderStatus(Context context,
                                         String userId,
                                         String orderId,
                                         String status) {
        AppPreferences prefs = new AppPreferences(context);
        if (!prefs.isOrderNotifEnabled()) return;

        String title   = "Order Update";
        String message = "Your order #" + orderId + " is now " + status;

        showNotification(context, NOTIF_ORDER_STATUS,
                CHANNEL_ORDERS, title, message);

        new NotificationRepository()
                .saveOrderNotification(userId, title, message, orderId);
    }


}