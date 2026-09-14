package lk.zenova.gomart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.zenova.gomart.helper.CartDatabaseHelper;
import lk.zenova.gomart.model.CartItem;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectivityReceiver";

    public static final String ACTION_CONNECTIVITY_CHANGED =
            "lk.zenova.gomart.CONNECTIVITY_CHANGED";
    public static final String EXTRA_IS_ONLINE = "is_online";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean online = isOnline(context);
        Log.d(TAG, "Connectivity changed — online: " + online);

        Intent broadcast = new Intent(ACTION_CONNECTIVITY_CHANGED);
        broadcast.putExtra(EXTRA_IS_ONLINE, online);
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(broadcast);

        if (online) {
            Log.d(TAG, "Internet restored — syncing cart to Firestore");
            syncCartToFirestore(context);
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return false;

        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    private void syncCartToFirestore(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        CartDatabaseHelper dbHelper = new CartDatabaseHelper(context);
        List<CartItem> unsyncedItems = dbHelper.getUnsyncedCartItems();

        if (unsyncedItems.isEmpty()) {
            Log.d(TAG, "No unsynced items");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (CartItem item : unsyncedItems) {
            Map<String, Object> cartData = new HashMap<>();
            cartData.put("productId", item.getProductId());
            cartData.put("quantity",  item.getQuantity());

            db.collection("users")
                    .document(uid)
                    .collection("cart")
                    .document(item.getProductId())
                    .set(cartData)
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Synced: " + item.getProductId());
                        dbHelper.markAllSynced();
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Sync failed: " + e.getMessage()));
        }
    }
}
