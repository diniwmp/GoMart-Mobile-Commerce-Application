package lk.zenova.gomart.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.CartItem;
import lk.zenova.gomart.model.Product;

public class CartAdapter extends
        RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private final List<CartItem>     cartItems;
    private final FragmentManager    fragmentManager;
    private OnQuantityChangeListener changeListener;
    private OnRemoveListener         removeListener;

    public CartAdapter(List<CartItem> cartItems,
                       FragmentManager fragmentManager) {
        this.cartItems       = cartItems;
        this.fragmentManager = fragmentManager;
    }

    public void setOnQuantityChangeListener(
            OnQuantityChangeListener listener) {
        this.changeListener = listener;
    }

    public void setOnRemoveListener(
            OnRemoveListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {
        CartItem cartItem = cartItems.get(position);

        FirebaseFirestore.getInstance()
                .collection("products")
                .whereEqualTo("productId",
                        cartItem.getProductId())
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds.isEmpty()) return;

                    int currentPosition =
                            holder.getAbsoluteAdapterPosition();
                    if (currentPosition ==
                            RecyclerView.NO_POSITION) return;

                    Product product = qds.getDocuments()
                            .get(0).toObject(Product.class);
                    if (product == null) return;

                    int stock = product.getStockCount();
                    boolean inStock = stock > 0;

                    holder.productTitle.setText(
                            product.getTitle());

                    holder.productTitle.setText(product.getTitle());

                    holder.unitPrice.setText(
                            String.format(Locale.US,
                                    "Unit Price: LKR %,.2f",
                                    product.getPrice()));

                    if (product.getImages() != null
                            && !product.getImages().isEmpty()) {
                        Glide.with(holder.itemView.getContext())
                                .load(product.getImages().get(0))
                                .centerCrop()
                                .into(holder.productImage);
                    }

                    if (!inStock) {

                        holder.productPrice.setVisibility(View.GONE);


                        holder.outOfStockBadge
                                .setVisibility(View.VISIBLE);

                        holder.outOfStockLabel
                                .setVisibility(View.VISIBLE);
                        holder.limitedStockLabel
                                .setVisibility(View.GONE);

                        holder.btnPlus.setEnabled(false);
                        holder.btnPlus.setAlpha(0.4f);
                        holder.btnMinus.setEnabled(false);
                        holder.btnMinus.setAlpha(0.4f);
                        holder.btnPlus.setOnClickListener(null);
                        holder.btnMinus.setOnClickListener(null);

                        holder.productQuantity.setText(
                                String.valueOf(
                                        cartItem.getQuantity()));
                        holder.productQuantity.setAlpha(0.4f);

                    } else {

                        holder.productPrice.setVisibility(View.VISIBLE);
                        holder.productPrice.setTextColor(
                                holder.itemView.getContext()
                                        .getColor(R.color.orange));


                        holder.outOfStockBadge
                                .setVisibility(View.GONE);

                        holder.outOfStockLabel
                                .setVisibility(View.GONE);
                        holder.productQuantity.setAlpha(1.0f);

                        if (cartItem.getQuantity() > stock) {
                            cartItem.setQuantity(stock);

                            if (changeListener != null)
                                changeListener.onChanged(cartItem);
                        }

                        int effectiveQty = cartItem.getQuantity();


                        if (stock <= 5) {
                            holder.limitedStockLabel
                                    .setVisibility(View.VISIBLE);
                            holder.limitedStockLabel.setText(
                                    "Only " + stock + " left in stock");
                        } else {
                            holder.limitedStockLabel
                                    .setVisibility(View.GONE);
                        }


                        holder.productPrice.setText(
                                String.format(Locale.US,
                                        "LKR %,.2f",
                                        product.getPrice()
                                                * effectiveQty));
                        holder.productPrice.setTextColor(
                                holder.itemView.getContext()
                                        .getColor(
                                                R.color.text_grey));

                        holder.productQuantity.setText(
                                String.valueOf(effectiveQty));

                        boolean atMax = effectiveQty >= stock;
                        holder.btnPlus.setEnabled(!atMax);
                        holder.btnPlus.setAlpha(
                                atMax ? 0.4f : 1.0f);
                        holder.btnMinus.setEnabled(true);
                        holder.btnMinus.setAlpha(1.0f);

                        holder.btnPlus.setOnClickListener(v -> {
                            int latestQty =
                                    cartItem.getQuantity();
                            if (latestQty < stock) {
                                cartItem.setQuantity(
                                        latestQty + 1);
                                notifyItemChanged(
                                        currentPosition);
                                if (changeListener != null)
                                    changeListener
                                            .onChanged(cartItem);
                            }
                        });

                        holder.btnMinus.setOnClickListener(v -> {
                            int latestQty =
                                    cartItem.getQuantity();
                            if (latestQty > 1) {
                                cartItem.setQuantity(
                                        latestQty - 1);
                                notifyItemChanged(
                                        currentPosition);
                                if (changeListener != null)
                                    changeListener
                                            .onChanged(cartItem);
                            }
                        });
                    }

                    holder.productImage.setOnClickListener(v -> {
                        android.os.Bundle bundle =
                                new android.os.Bundle();
                        bundle.putString("productId",
                                product.getProductId());
                        lk.zenova.gomart.fragment
                                .ProductDetailsFragment details =
                                new lk.zenova.gomart.fragment
                                        .ProductDetailsFragment();
                        details.setArguments(bundle);
                        fragmentManager.beginTransaction()
                                .replace(R.id.fragment_container,
                                        details)
                                .addToBackStack(null)
                                .commit();
                    });

                    holder.btnRemove.setOnClickListener(v -> {
                        int pos = holder
                                .getAbsoluteAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION
                                && removeListener != null) {
                            removeListener.onRemoved(pos);
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("CartAdapter",
                                "Error: " + e.getMessage()));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {
        ImageView       productImage;
        TextView        productTitle;
        TextView        productPrice;
        TextView        productQuantity;
        AppCompatButton btnPlus;
        AppCompatButton btnMinus;
        ImageView       btnRemove;
        TextView        outOfStockBadge;
        TextView unitPrice;

        TextView        outOfStockLabel;
        TextView        limitedStockLabel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            unitPrice = itemView.findViewById(
                    R.id.item_cart_unit_price);

            productImage      = itemView.findViewById(
                    R.id.item_cart_image);
            productTitle      = itemView.findViewById(
                    R.id.item_cart_title);
            productPrice      = itemView.findViewById(
                    R.id.item_cart_price);
            productQuantity   = itemView.findViewById(
                    R.id.item_cart_quantity);
            btnPlus           = itemView.findViewById(
                    R.id.item_cart_btn_plus);
            btnMinus          = itemView.findViewById(
                    R.id.item_cart_btn_minus);
            btnRemove         = itemView.findViewById(
                    R.id.item_cart_remove);
            outOfStockBadge   = itemView.findViewById(
                    R.id.item_cart_out_of_stock);



            outOfStockLabel   = itemView.findViewById(
                    R.id.item_cart_oos_label);
            limitedStockLabel = itemView.findViewById(
                    R.id.item_cart_limited_stock);
        }
    }



    public interface OnQuantityChangeListener {
        void onChanged(CartItem cartItem);
    }

    public interface OnRemoveListener {
        void onRemoved(int position);
    }
}