package lk.zenova.gomart.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.SuggestionItem;

public class SearchSuggestionAdapter extends ArrayAdapter<SuggestionItem> {

    private List<SuggestionItem> allItems;
    private List<SuggestionItem> filteredItems;

    public SearchSuggestionAdapter(Context context,
                                   List<SuggestionItem> items) {
        super(context, 0, items);
        this.allItems      = new ArrayList<>(items);
        this.filteredItems = new ArrayList<>(items);
    }

    public void updateAllItems(List<SuggestionItem> newItems) {
        this.allItems = new ArrayList<>(newItems);
        this.filteredItems = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Nullable
    @Override
    public SuggestionItem getItem(int position) {
        return filteredItems.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView,
                        @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_autocomplete_suggestion,
                            parent, false);
        }

        SuggestionItem item = filteredItems.get(position);

        ImageView icon    = convertView.findViewById(R.id.suggestion_icon);
        TextView  text    = convertView.findViewById(R.id.suggestion_text);
        TextView  typeBadge = convertView.findViewById(R.id.suggestion_type);

        text.setText(item.getDisplayName());

        switch (item.getType()) {
            case SuggestionItem.TYPE_PRODUCT:
                icon.setImageResource(R.drawable.shopping_cart_24);
                typeBadge.setText("Product");
                typeBadge.setTextColor(
                        android.graphics.Color.parseColor("#FF6600"));
                break;
            case SuggestionItem.TYPE_CATEGORY:
                icon.setImageResource(R.drawable.list_24);
                typeBadge.setText("Category");
                typeBadge.setTextColor(
                        android.graphics.Color.parseColor("#2196F3"));
                break;
            case SuggestionItem.TYPE_BRAND:
                icon.setImageResource(R.drawable.promo_24);
                typeBadge.setText("Brand");
                typeBadge.setTextColor(
                        android.graphics.Color.parseColor("#4CAF50"));
                break;
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    results.values = new ArrayList<>(allItems);
                    results.count  = allItems.size();
                    return results;
                }

                String query = constraint.toString()
                        .toLowerCase().trim();
                List<SuggestionItem> filtered = new ArrayList<>();

                for (SuggestionItem item : allItems) {
                    if (item.getDisplayName()
                            .toLowerCase().contains(query)) {
                        filtered.add(item);
                    }
                }

                if (filtered.size() > 8) {
                    filtered = filtered.subList(0, 8);
                }

                results.values = filtered;
                results.count  = filtered.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                filteredItems = (List<SuggestionItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}