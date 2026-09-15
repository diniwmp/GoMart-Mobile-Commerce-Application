package lk.zenova.gomart.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Product;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.ViewHolder> {

    private List<Product> products;
    private OnListingItemClickListener listener;
    private OnAddToCartClickListener cartListener;

    public SectionAdapter(List<Product> products, OnListingItemClickListener listener,
                          OnAddToCartClickListener cartListener) {
        this.products = products;
        this.listener = listener;
        this.cartListener = cartListener;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_recycler, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {
        Product product = products.get(position);
        holder.productTitle.setText(product.getTitle());
        holder.productPrice.setText("LKR " + product.getPrice());

        if (product.getImages() != null
                && !product.getImages().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImages().get(0))
                    .centerCrop()
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(
                    R.drawable.shopping_cart_unfilled);
        }

        int stock = product.getStockCount();

        if (stock <= 0) {
            holder.addToCartBtn.setEnabled(false);
            holder.addToCartBtn.setAlpha(0.4f);
            holder.addToCartBtn.setIconResource(R.drawable.add_24);

            holder.outOfStockLabel.setVisibility(View.VISIBLE);

        } else {
            holder.addToCartBtn.setEnabled(true);
            holder.addToCartBtn.setAlpha(1.0f);
            holder.addToCartBtn.setIconResource(R.drawable.add_24);

            holder.outOfStockLabel.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Animation animation = AnimationUtils.loadAnimation(
                    v.getContext(), R.anim.click_animation);
            v.startAnimation(animation);
            if (listener != null) listener.onListingItemClick(product);
        });

        holder.addToCartBtn.setOnClickListener(v -> {
            Animation animation = AnimationUtils.loadAnimation(
                    v.getContext(), R.anim.click_animation);
            v.startAnimation(animation);
            if (cartListener != null)
                cartListener.onAddToCartClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        TextView outOfStockLabel;
        MaterialButton addToCartBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_product_r_image);
            productTitle = itemView.findViewById(R.id.item_product_r_name);
            productPrice = itemView.findViewById(R.id.item_product_r_price);
            addToCartBtn   = itemView.findViewById(R.id.item_product_r_add_btn);
            outOfStockLabel  = itemView.findViewById(R.id.item_product_r_out_of_stock);

        }
    }

    public interface OnListingItemClickListener {
        void onListingItemClick(Product product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(Product product);
    }
}

