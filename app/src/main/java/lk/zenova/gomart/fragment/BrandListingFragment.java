package lk.zenova.gomart.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.zenova.gomart.R;
import lk.zenova.gomart.activity.SignInActivity;
import lk.zenova.gomart.adapter.SectionAdapter;
import lk.zenova.gomart.databinding.FragmentListingBinding;
import lk.zenova.gomart.model.Product;


public class BrandListingFragment extends Fragment {

    private FragmentListingBinding binding;
    private FirebaseAuth           auth;
    private FirebaseFirestore      db;

    private String brandId;
    private String brandName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            brandId   = getArguments().getString("brandId");
            brandName = getArguments().getString("brandName", "Products");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        binding.listingBtnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        getActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                requireActivity()
                                        .getSupportFragmentManager()
                                        .popBackStack();
                            }
                        });

        binding.listingTitle.setText(
                brandName != null ? brandName : "Products");

        binding.listingRecycler.setLayoutManager(
                new GridLayoutManager(getContext(), 2));

        if (brandId == null || brandId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Brand not found", Toast.LENGTH_SHORT).show();
            return;
        }

        loadProducts();
    }

    private void loadProducts() {
        binding.listingProgress.setVisibility(View.VISIBLE);
        binding.listingEmpty.setVisibility(View.GONE);

        db.collection("products")
                .whereEqualTo("brandId", brandId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;
                    binding.listingProgress.setVisibility(View.GONE);

                    if (snapshot == null || snapshot.isEmpty()) {
                        binding.listingEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<Product> products = snapshot.toObjects(Product.class);

                    products.sort((a, b) -> {
                        String ta = a.getTitle() != null ? a.getTitle() : "";
                        String tb = b.getTitle() != null ? b.getTitle() : "";
                        return ta.compareToIgnoreCase(tb);
                    });

                    setupAdapter(products);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.listingProgress.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Failed to load products",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupAdapter(List<Product> products) {
        SectionAdapter adapter = new SectionAdapter(
                products,

                product -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("productId", product.getProductId());

                    ProductDetailsFragment details =
                            new ProductDetailsFragment();
                    details.setArguments(bundle);

                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, details)
                            .addToBackStack(null)
                            .commit();
                },

                product -> addToCart(product)
        );

        binding.listingRecycler.setAdapter(adapter);
    }

    private void addToCart(Product product) {
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), SignInActivity.class));
            return;
        }

        String uid      = auth.getCurrentUser().getUid();
        String imageUrl = (product.getImages() != null
                && !product.getImages().isEmpty())
                ? product.getImages().get(0) : "";

        db.collection("users").document(uid)
                .collection("cart")
                .document(product.getProductId())
                .get()
                .addOnSuccessListener(doc -> {
                    int currentQty = 0;
                    if (doc.exists()) {
                        Long qty = doc.getLong("quantity");
                        currentQty = qty != null ? qty.intValue() : 0;
                    }

                    if (currentQty >= product.getStockCount()) {
                        Toast.makeText(getContext(),
                                "Max stock reached! Only "
                                        + product.getStockCount() + " available.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newQty = currentQty + 1;

                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productId", product.getProductId());
                    cartItem.put("title",     product.getTitle());
                    cartItem.put("price",     product.getPrice());
                    cartItem.put("imageUrl",  imageUrl);
                    cartItem.put("quantity",  newQty);

                    db.collection("users").document(uid)
                            .collection("cart")
                            .document(product.getProductId())
                            .set(cartItem)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(getContext(),
                                            product.getTitle() + " added! (Qty: "
                                                    + newQty + ")",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed to add to cart",
                                            Toast.LENGTH_SHORT).show());
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null)
            getActivity().findViewById(R.id.bottom_navigation_view)
                    .setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().findViewById(R.id.bottom_navigation_view)
                    .setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
