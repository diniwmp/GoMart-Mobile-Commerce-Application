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
import lk.zenova.gomart.adapter.BrandAdapter;
import lk.zenova.gomart.databinding.FragmentBrandBinding;
import lk.zenova.gomart.model.Brand;

public class BrandFragment extends Fragment {

    private static final String TAG = "BrandFragment";
    private FragmentBrandBinding binding;
    private BrandAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBrandBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewBrands.setLayoutManager(new GridLayoutManager(getContext(), 3));

        loadBrands();
    }

    private void loadBrands() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("brands").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) return;

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.w(TAG, "No brands found");
                        Toast.makeText(getContext(), "No brands found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Brand> brands = querySnapshot.toObjects(Brand.class);
                    Log.i(TAG, "Loaded " + brands.size() + " brands");

                    adapter = new BrandAdapter(brands, brand -> {
                        if (brand == null || brand.getBrandId() == null) {
                            Log.e(TAG, "Brand or brandId is null");
                            return;
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString("brandId", brand.getBrandId());
                        bundle.putString("brandName", brand.getBrandName());

                        BrandListingFragment fragment = new BrandListingFragment();
                        fragment.setArguments(bundle);

                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });

                    binding.recyclerViewBrands.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load brands: " + e.getMessage(), e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load brands", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}