package lk.zenova.gomart.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Order;
import lk.zenova.gomart.model.Product;

public class OrderDetailProductAdapter extends RecyclerView.Adapter<OrderDetailProductAdapter.ViewHolder> {

    private List<Order.OrderItem> orderItems;

    public OrderDetailProductAdapter(List<Order.OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order.OrderItem orderItem = orderItems.get(position);

        holder.qty.setText("Qty: " + orderItem.getQuantity());
        holder.price.setText(String.format(Locale.US, "LKR %,.2f",
                orderItem.getUnitPrice() * orderItem.getQuantity()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .whereEqualTo("productId", orderItem.getProductId())
                .get()
                .addOnSuccessListener(qds -> {
                    if (!qds.isEmpty()) {
                        int currentPos = holder.getAbsoluteAdapterPosition();
                        if (currentPos == RecyclerView.NO_POSITION) return;

                        Product product = qds.getDocuments().get(0).toObject(Product.class);
                        if (product == null) return;

                        holder.title.setText(product.getTitle());

                        if (product.getImages() != null && !product.getImages().isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(product.getImages().get(0))
                                    .centerCrop()
                                    .placeholder(R.drawable.shopping_bag_24)
                                    .into(holder.image);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("OrderDetailAdapter", "Error: " + e.getMessage()));
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, qty, price;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.order_product_image);
            title = itemView.findViewById(R.id.order_product_title);
            qty = itemView.findViewById(R.id.order_product_qty);
            price = itemView.findViewById(R.id.order_product_price);
        }
    }
}