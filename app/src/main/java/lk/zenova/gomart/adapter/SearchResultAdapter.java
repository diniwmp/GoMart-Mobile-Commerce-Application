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
import lk.zenova.gomart.model.Category;
import lk.zenova.gomart.model.Product;
import lk.zenova.gomart.model.SearchResultItem;

public class SearchResultAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSearchResultClickListener {
        void onProductClick(Product product);
        void onCategoryClick(Category category);
        void onBrandClick(Brand brand);
    }

    private List<SearchResultItem> items;
    private final OnSearchResultClickListener listener;

    public SearchResultAdapter(List<SearchResultItem> items,
                               OnSearchResultClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    public void updateItems(List<SearchResultItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == SearchResultItem.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_search_header, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_search_result, parent, false);
        return new ResultViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {
        SearchResultItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(item.getHeaderTitle());
            return;
        }

        ResultViewHolder vh = (ResultViewHolder) holder;

        android.util.TypedValue tv = new android.util.TypedValue();

        if (item.getType() == SearchResultItem.TYPE_PRODUCT) {
            Product p = (Product) item.getData();

            vh.title.setText(p.getTitle() != null ? p.getTitle() : "");
            vh.subtitle.setText("Rs. " + String.format("%.2f", p.getPrice()));

            vh.badge.setText("Product");
            vh.badge.setTextColor(
                    android.graphics.Color.parseColor("#64748B"));
            vh.itemView.getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
            vh.badge.setBackgroundColor(tv.data);
            vh.badge.setVisibility(View.VISIBLE);

            if (p.getImages() != null && !p.getImages().isEmpty()) {
                Glide.with(vh.icon.getContext())
                        .load(p.getImages().get(0))
                        .centerCrop()
                        .placeholder(R.drawable.shopping_bag_24)
                        .into(vh.icon);
            } else {
                vh.icon.setImageResource(R.drawable.shopping_bag_24);
            }

            vh.itemView.setOnClickListener(v -> listener.onProductClick(p));

        } else if (item.getType() == SearchResultItem.TYPE_CATEGORY) {
            Category c = (Category) item.getData();

            vh.title.setText(c.getName() != null ? c.getName() : "");
            vh.subtitle.setText("Browse products");

            vh.badge.setText("Category");
            vh.badge.setTextColor(
                    android.graphics.Color.parseColor("#C2410C"));
            vh.itemView.getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
            vh.badge.setBackgroundColor(tv.data);
            vh.badge.setVisibility(View.VISIBLE);

            if (c.getImageUrl() != null && !c.getImageUrl().isEmpty()) {
                Glide.with(vh.icon.getContext())
                        .load(c.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.grocery_24)
                        .into(vh.icon);
            } else {
                vh.icon.setImageResource(R.drawable.grocery_24);
            }

            vh.itemView.setOnClickListener(v -> listener.onCategoryClick(c));

        } else if (item.getType() == SearchResultItem.TYPE_BRAND) {
            Brand b = (Brand) item.getData();

            vh.title.setText(b.getBrandName() != null ? b.getBrandName() : "");
            vh.subtitle.setText("View brand products");

            vh.badge.setText("Brand");
            vh.badge.setTextColor(
                    android.graphics.Color.parseColor("#1D4ED8"));
            vh.itemView.getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
            vh.badge.setBackgroundColor(tv.data);
            vh.badge.setVisibility(View.VISIBLE);

            if (b.getImageUrl() != null && !b.getImageUrl().isEmpty()) {
                Glide.with(vh.icon.getContext())
                        .load(b.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.shopping_cart_unfilled)
                        .into(vh.icon);
            } else {
                vh.icon.setImageResource(R.drawable.shopping_cart_unfilled);
            }

            vh.itemView.setOnClickListener(v -> listener.onBrandClick(b));
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        HeaderViewHolder(View v) {
            super(v);
            title = (TextView) v;
        }
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView  title, subtitle, badge;
        ResultViewHolder(View v) {
            super(v);
            icon     = v.findViewById(R.id.search_result_icon);
            title    = v.findViewById(R.id.search_result_title);
            subtitle = v.findViewById(R.id.search_result_subtitle);
            badge    = v.findViewById(R.id.search_result_badge);
        }
    }
}