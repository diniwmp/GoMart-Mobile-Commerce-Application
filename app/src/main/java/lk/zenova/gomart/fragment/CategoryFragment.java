package lk.zenova.gomart.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.adapter.CategoryAdapter;
import lk.zenova.gomart.databinding.FragmentCategoryBinding;
import lk.zenova.gomart.model.Category;

public class CategoryFragment extends Fragment {

    private static final String TAG = "CategoryFragment";
    private FragmentCategoryBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewCategories.setLayoutManager(
                new GridLayoutManager(getContext(), 3));

        loadCategories();
    }

    private void loadCategories() {
        FirebaseFirestore.getInstance()
                .collection("categories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;

                    if (snapshot == null || snapshot.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No categories found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Category> categories =
                            snapshot.toObjects(Category.class);
                    Log.i(TAG, "Loaded " + categories.size() + " categories");

                    CategoryAdapter adapter = new CategoryAdapter(
                            categories, category -> {
                        if (category == null
                                || category.getCategoryId() == null) return;

                        Bundle bundle = new Bundle();
                        bundle.putString("categoryId",
                                category.getCategoryId());
                        bundle.putString("categoryName",
                                category.getName());

                        ListingFragment fragment = new ListingFragment();
                        fragment.setArguments(bundle);

                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });

                    binding.recyclerViewCategories.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed: " + e.getMessage());
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Failed to load categories",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}