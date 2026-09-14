package lk.zenova.gomart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import lk.zenova.gomart.helper.NotificationHelper;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d("NotificationReceiver", "Action: " + action);

        switch (action) {

            case "ACTION_ORDER_PLACED":
                String orderId = intent.getStringExtra("orderId");
                String userId1 = intent.getStringExtra("userId");
                NotificationHelper.notifyOrderPlaced(context,
                        userId1 != null ? userId1 : "",
                        orderId != null ? orderId : "");
                break;

            case "ACTION_ORDER_STATUS":
                String oId    = intent.getStringExtra("orderId");
                String userId2 = intent.getStringExtra("userId");
                String status  = intent.getStringExtra("status");
                NotificationHelper.notifyOrderStatus(context,
                        userId2 != null ? userId2 : "",
                        oId != null ? oId : "",
                        status != null ? status : "Updated");
                break;

            case "ACTION_PAYMENT":
                String pOId   = intent.getStringExtra("orderId");
                double amount = intent.getDoubleExtra("amount", 0);
                NotificationHelper.notifyPaymentConfirmed(context,
                        pOId != null ? pOId : "", amount);
                break;



        }
    }
}