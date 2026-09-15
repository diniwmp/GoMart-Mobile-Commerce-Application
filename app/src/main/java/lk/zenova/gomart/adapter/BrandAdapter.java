package lk.zenova.gomart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Brand;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.ViewHolder> {

    private List<Brand> brands;
    private OnBrandClickListener listener;

    public BrandAdapter(List<Brand> brands, OnBrandClickListener listener) {
        this.brands = brands;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_brand, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Brand brand = brands.get(position);
        holder.brandName.setText(brand.getBrandName());

        String imageUrl = brand.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.shopping_cart_unfilled)
                    .into(holder.brandImage);
        } else {
            holder.brandImage.setImageResource(R.drawable.shopping_cart_unfilled);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBrandClick(brand);
            }
        });
    }

    @Override
    public int getItemCount() {
        return brands.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView brandImage;
        TextView brandName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            brandImage = itemView.findViewById(R.id.brand_image);
            brandName = itemView.findViewById(R.id.brand_name);
        }
    }

    public interface OnBrandClickListener {
        void onBrandClick(Brand brand);
    }
}