package lk.zenova.gomart.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Product;
import lk.zenova.gomart.model.Wishlist;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private final List<Wishlist> wishlistItems;
    private OnRemoveListener removeListener;
    private OnItemClickListener itemClickListener;
    private OnAddToCartListener addToCartListener;

    public WishlistAdapter(List<Wishlist> wishlistItems) {
        this.wishlistItems = wishlistItems;
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.addToCartListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wishlist wishlistItem = wishlistItems.get(position);

        holder.cardView.setTranslationX(0f);
        holder.swipeActionsLayout.setVisibility(View.INVISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .whereEqualTo("productId", wishlistItem.getProductId())
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds.isEmpty()) return;

                    int pos = holder.getAbsoluteAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    Product product = qds.getDocuments()
                            .get(0).toObject(Product.class);
                    if (product == null) return;

                    holder.productTitle.setText(product.getTitle());
                    holder.productPrice.setText(
                            String.format(Locale.US,
                                    "LKR %,.2f", product.getPrice()));

                    if (product.getImages() != null
                            && !product.getImages().isEmpty()) {
                        Glide.with(holder.itemView.getContext())
                                .load(product.getImages().get(0))
                                .centerCrop()
                                .into(holder.productImage);
                    }

                    int stock = product.getStockCount();
                    boolean inStock = stock > 0;

                    if (inStock) {
                        holder.swipeBtnAddCart.setAlpha(1.0f);
                        holder.swipeBtnAddCartText.setText("Add to Cart");
                        holder.swipeBtnAddCartIcon.setImageResource(
                                R.drawable.shopping_cart_24);

                    } else {
                        holder.swipeBtnAddCart.setAlpha(0.4f);
                        holder.swipeBtnAddCartText.setText("Out of Stock");
                        holder.swipeBtnAddCartIcon.setImageResource(
                                R.drawable.shopping_cart_24);
                    }

                    if (inStock) {
                        holder.outOfStockBadge.setVisibility(View.GONE);
                    } else {
                        holder.outOfStockBadge.setVisibility(View.VISIBLE);
                    }

                    holder.itemView.setOnClickListener(v -> {
                        if (itemClickListener != null)
                            itemClickListener.onItemClick(product);
                    });

                    float[] dX = {0f};
                    float MAX_SWIPE = -160 * holder.itemView.getContext()
                            .getResources().getDisplayMetrics().density;

                    holder.cardView.setOnTouchListener((v, event) -> {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                dX[0] = holder.cardView.getTranslationX()
                                        - event.getRawX();
                                holder.swipeActionsLayout
                                        .setVisibility(View.VISIBLE);
                                return true;

                            case MotionEvent.ACTION_MOVE:
                                float newX = event.getRawX() + dX[0];
                                if (newX <= 0 && newX >= MAX_SWIPE) {
                                    holder.cardView.setTranslationX(newX);
                                } else if (newX < MAX_SWIPE) {
                                    holder.cardView.setTranslationX(MAX_SWIPE);
                                }
                                return true;

                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                float currentX = holder.cardView
                                        .getTranslationX();
                                if (currentX < MAX_SWIPE / 2) {
                                    holder.cardView.animate()
                                            .translationX(MAX_SWIPE)
                                            .setDuration(150).start();
                                    holder.swipeActionsLayout
                                            .setVisibility(View.VISIBLE);
                                } else {
                                    holder.cardView.animate()
                                            .translationX(0f)
                                            .setDuration(150)
                                            .withEndAction(() ->
                                                    holder.swipeActionsLayout
                                                            .setVisibility(
                                                                    View.INVISIBLE))
                                            .start();
                                }
                                return true;
                        }
                        return false;
                    });

                    holder.swipeBtnRemove.setOnClickListener(v -> {
                        holder.cardView.animate()
                                .translationX(0f)
                                .setDuration(150)
                                .withEndAction(() -> {
                                    int p = holder
                                            .getAbsoluteAdapterPosition();
                                    if (p != RecyclerView.NO_POSITION
                                            && removeListener != null)
                                        removeListener.onRemoved(p);
                                }).start();
                    });

                    holder.swipeBtnAddCart.setOnClickListener(v -> {
                        if (!inStock) {
                            android.widget.Toast.makeText(
                                            holder.itemView.getContext(),
                                            product.getTitle()
                                                    + " is currently out of stock",
                                            android.widget.Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        holder.cardView.animate()
                                .translationX(0f)
                                .setDuration(150)
                                .withEndAction(() -> {
                                    if (addToCartListener != null)
                                        addToCartListener.onAddToCart(product);
                                }).start();
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("WishlistAdapter",
                                "Error: " + e.getMessage()));
    }

    @Override
    public int getItemCount() {
        return wishlistItems.size();
    }

    public void removeItem(int position) {
        if (position < 0 || position >= wishlistItems.size()) return;
        wishlistItems.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, wishlistItems.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        LinearLayout swipeActionsLayout;
        LinearLayout swipeBtnRemove;
        LinearLayout swipeBtnAddCart;
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;

        TextView outOfStockBadge;
        ImageView swipeBtnAddCartIcon;
        TextView swipeBtnAddCartText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView           = itemView.findViewById(R.id.wishlist_item_card);
            swipeActionsLayout = itemView.findViewById(R.id.swipe_actions_layout);
            swipeBtnRemove     = itemView.findViewById(R.id.swipe_btn_remove);
            swipeBtnAddCart    = itemView.findViewById(R.id.swipe_btn_add_cart);
            productImage       = itemView.findViewById(R.id.item_wishlist_image);
            productTitle       = itemView.findViewById(R.id.item_wishlist_title);
            productPrice       = itemView.findViewById(R.id.item_wishlist_price);
            swipeBtnAddCartIcon = itemView.findViewById(R.id.swipe_cart_icon);
            swipeBtnAddCartText = itemView.findViewById(R.id.swipe_cart_text);
            outOfStockBadge     = itemView.findViewById(R.id.wishlist_out_of_stock);
        }
    }

    public interface OnRemoveListener {
        void onRemoved(int position);
    }

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }


    public interface OnAddToCartListener {
        void onAddToCart(Product product);
    }

}