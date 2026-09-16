package lk.zenova.gomart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Order;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {

    private List<Order> orders;
    private OnOrderClickListener listener;

    public OrdersAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {
        Order order = orders.get(position);

        String shortId = order.getOrderId();
        if (shortId != null && shortId.length() > 8) {
            shortId = "#" + shortId.substring(
                    shortId.length() - 8);
        }
        holder.orderId.setText(shortId);

        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.orderDate.setText(
                    sdf.format(order.getOrderDate().toDate()));
        }

        String status = order.getStatus();
        holder.orderStatus.setText(
                status != null ? status.toUpperCase() : "PENDING");
        setStatusColor(holder.orderStatus,
                status != null ? status.toUpperCase() : "PENDING");

        int itemCount = order.getOrderItems() != null
                ? order.getOrderItems().size() : 0;
        holder.itemsCount.setText(itemCount
                + (itemCount == 1 ? " Item" : " Items"));

        holder.total.setText(String.format(Locale.US,
                "LKR %,.2f", order.getTotalAmount()));


        boolean hasBillingAddress =
                order.getBillingAddress() != null
                        && order.getBillingAddress().getAddress() != null
                        && !order.getBillingAddress()
                        .getAddress().isEmpty();

        Order.Address displayAddress = hasBillingAddress
                ? order.getBillingAddress()
                : order.getShippingAddress();

        if (displayAddress != null) {
            String addressText = "";
            if (displayAddress.getAddressName() != null
                    && !displayAddress.getAddressName().isEmpty()) {
                addressText = displayAddress.getAddressName()
                        + " — ";
            }
            if (displayAddress.getAddress() != null) {
                addressText += displayAddress.getAddress();
            }
            holder.address.setText(addressText);
        } else {
            holder.address.setText("No address");
        }

        holder.btnDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    private void setStatusColor(TextView statusView, String status) {
        if (status == null) return;
        switch (status.toUpperCase()) {
            case "PAID":
                statusView.setBackgroundResource(R.drawable.bg_status_paid);
                break;
            case "PROCESSING":
                statusView.setBackgroundResource(R.drawable.bg_status_processing);
                break;
            case "SHIPPED":
                statusView.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            case "DELIVERED":
                statusView.setBackgroundResource(R.drawable.bg_status_delivered);
                break;
            case "CANCELLED":
                statusView.setBackgroundResource(R.drawable.bg_status_cancelled);
                break;
            default:
                statusView.setBackgroundResource(R.drawable.bg_status_badge);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderDate, orderStatus, itemsCount, total, address;
        MaterialButton btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId     = itemView.findViewById(R.id.item_order_id);
            orderDate   = itemView.findViewById(R.id.item_order_date);
            orderStatus = itemView.findViewById(R.id.item_order_status);
            itemsCount  = itemView.findViewById(R.id.item_order_items_count);
            total       = itemView.findViewById(R.id.item_order_total);
            address     = itemView.findViewById(R.id.item_order_address);
            btnDetails  = itemView.findViewById(R.id.item_order_btn_details);
        }
    }

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }
}