package lk.zenova.gomart.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.adapter.WishlistAdapter;
import lk.zenova.gomart.databinding.FragmentWishlistBinding;
import lk.zenova.gomart.model.CartItem;
import lk.zenova.gomart.model.Wishlist;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private List<Wishlist> wishlistItems;
    private WishlistAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            binding.wishlistItems.setVisibility(View.GONE);
            binding.wishlistEmpty.setVisibility(View.VISIBLE);
            return;
        }

        uid = firebaseAuth.getCurrentUser().getUid();
        loadWishlist();
    }

    private void loadWishlist() {
        db.collection("users").document(uid)
                .collection("wishlist")
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;

                    if (!qds.isEmpty()) {
                        wishlistItems = new ArrayList<>();

                        for (DocumentSnapshot ds : qds.getDocuments()) {
                            Wishlist item = ds.toObject(Wishlist.class);
                            if (item != null) {
                                item.setDocumentId(ds.getId());
                                wishlistItems.add(item);
                            }
                        }

                        binding.wishlistEmpty.setVisibility(View.GONE);
                        binding.wishlistItems.setVisibility(View.VISIBLE);

                        binding.wishlistItems.setLayoutManager(
                                new LinearLayoutManager(getContext()));

                        adapter = new WishlistAdapter(wishlistItems);
                        setupAdapterListeners();
                        binding.wishlistItems.setAdapter(adapter);

                    } else {
                        binding.wishlistItems.setVisibility(View.GONE);
                        binding.wishlistEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(),
                                "Failed to load wishlist",
                                Toast.LENGTH_SHORT).show();
                });
    }

    private void setupAdapterListeners() {

        adapter.setOnRemoveListener(position -> {
            if (position >= wishlistItems.size()) return;
            String documentId = wishlistItems.get(position).getDocumentId();

            db.collection("users").document(uid)
                    .collection("wishlist")
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (binding == null) return;
                        adapter.removeItem(position);
                        Toast.makeText(getContext(),
                                "Removed from wishlist",
                                Toast.LENGTH_SHORT).show();

                        if (wishlistItems.isEmpty()) {
                            binding.wishlistItems.setVisibility(View.GONE);
                            binding.wishlistEmpty.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null)
                            Toast.makeText(getContext(),
                                    "Failed to remove",
                                    Toast.LENGTH_SHORT).show();
                    });
        });

        adapter.setOnItemClickListener(product -> {
            Bundle bundle = new Bundle();
            bundle.putString("productId", product.getProductId());

            ProductDetailsFragment fragment = new ProductDetailsFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        adapter.setOnAddToCartListener(product -> {
            db.collection("users").document(uid)
                    .collection("cart")
                    .document(product.getProductId())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (binding == null) return;
                        if (snapshot.exists()) {
                            Toast.makeText(getContext(),
                                    product.getTitle() + " is already in your cart!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            CartItem cartItem = new CartItem(
                                    product.getProductId(), 1, null);
                            db.collection("users").document(uid)
                                    .collection("cart")
                                    .document(product.getProductId())
                                    .set(cartItem)
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(getContext(),
                                                    product.getTitle() + " added to cart!",
                                                    Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(),
                                                    "Failed to add to cart",
                                                    Toast.LENGTH_SHORT).show());
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Error checking cart",
                                    Toast.LENGTH_SHORT).show());
        });

    }

    private void setButtonInCart(MaterialButton btn) {
        btn.setText("In Cart");
        btn.setBackgroundTintList(
                android.content.res.ColorStateList
                        .valueOf(android.graphics.Color.parseColor("#4CAF50")));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}