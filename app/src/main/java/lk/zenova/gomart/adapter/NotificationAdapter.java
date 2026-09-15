package lk.zenova.gomart.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Notification;

public class NotificationAdapter extends
        RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<Notification> notifications;
    private final OnNotificationClickListener clickListener;
    private OnNotificationClearListener clearListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public interface OnNotificationClearListener {
        void onNotificationClear(Notification notification, int position);
    }

    public NotificationAdapter(List<Notification> notifications,
                               OnNotificationClickListener clickListener) {
        this.notifications = notifications;
        this.clickListener = clickListener;
    }

    public void setClearListener(OnNotificationClearListener clearListener) {
        this.clearListener = clearListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notif = notifications.get(position);
        Context ctx = holder.itemView.getContext();

        TypedValue tv = new TypedValue();

        ctx.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, tv, true);
        int colorOnSurface = tv.data;

        ctx.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
        int colorSurfaceVariant = tv.data;

        holder.title.setText(notif.getTitle());
        holder.message.setText(notif.getMessage());
        holder.time.setText(getTimeAgo(notif.getTimestamp()));

        if (!notif.isRead()) {
            holder.unreadDot.setVisibility(View.VISIBLE);
            holder.title.setTypeface(null, Typeface.BOLD);
            holder.title.setTextColor(
                    ContextCompat.getColor(ctx, R.color.orange));
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.unreadDot.setVisibility(View.GONE);
            holder.title.setTypeface(null, Typeface.BOLD);
            holder.title.setTextColor(colorOnSurface);
            holder.itemView.setAlpha(0.7f);
        }

        holder.iconBg.setBackgroundResource(R.drawable.bg_notif_icon);

        switch (notif.getType() != null ? notif.getType() : "SYSTEM") {

            case "ORDER":
                boolean isDark = (ctx.getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

                if (isDark) {
                    holder.iconBg.getBackground().setTint(
                            android.graphics.Color.WHITE);
                    holder.icon.setColorFilter(
                            android.graphics.Color.BLACK);
                } else {
                    holder.iconBg.getBackground().setTint(
                            android.graphics.Color.parseColor("#6B7280"));
                    holder.icon.setColorFilter(
                            android.graphics.Color.WHITE);
                }
                holder.icon.setImageResource(R.drawable.shopping_cart_24);
                break;

            case "PROMO":
            case "SALE":
                holder.iconBg.getBackground().setTint(
                        android.graphics.Color.parseColor("#FF6600"));
                holder.icon.setImageResource(R.drawable.promo_24);
                holder.icon.setColorFilter(
                        ContextCompat.getColor(ctx, R.color.white));
                break;

            case "SYSTEM":
            default:
                holder.iconBg.getBackground().setTint(
                        android.graphics.Color.parseColor("#2196F3"));
                holder.icon.setImageResource(R.drawable.settings_24);
                holder.icon.setColorFilter(
                        ContextCompat.getColor(ctx, R.color.white));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null)
                clickListener.onNotificationClick(notif);
        });

        holder.btnClear.setOnClickListener(v -> {
            if (clearListener != null)
                clearListener.onNotificationClear(
                        notif, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String getTimeAgo(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        long now     = System.currentTimeMillis();
        long time    = timestamp.toDate().getTime();
        long diff    = now - time;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours   = TimeUnit.MILLISECONDS.toHours(diff);
        long days    = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + " min ago";
        if (hours < 24)   return hours + " hr ago";
        if (days < 7)     return days + " days ago";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(time));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView     title, message, time;
        View         unreadDot;
        ImageView    icon, btnClear;
        LinearLayout iconBg;

        ViewHolder(@NonNull View v) {
            super(v);
            title     = v.findViewById(R.id.notif_title);
            message   = v.findViewById(R.id.notif_message);
            time      = v.findViewById(R.id.notif_time);
            unreadDot = v.findViewById(R.id.notif_unread_dot);
            icon      = v.findViewById(R.id.notif_icon);
            iconBg    = v.findViewById(R.id.notif_icon_bg);
            btnClear  = v.findViewById(R.id.notif_btn_clear);
        }
    }
}