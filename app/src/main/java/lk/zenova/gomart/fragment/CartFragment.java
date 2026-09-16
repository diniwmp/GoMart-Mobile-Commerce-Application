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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.zenova.gomart.R;
import lk.zenova.gomart.adapter.CartAdapter;
import lk.zenova.gomart.databinding.FragmentCartBinding;
import lk.zenova.gomart.model.CartItem;
import lk.zenova.gomart.model.Product;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private List<CartItem> cartItems;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        updateTotal();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).collection("cart").get()
                    .addOnSuccessListener(qds -> {

                        if (binding == null) return;

                        if (!qds.isEmpty()) {
                            cartItems = new ArrayList<>();

                            for (DocumentSnapshot ds : qds.getDocuments()) {
                                CartItem cartItem = ds.toObject(CartItem.class);
                                if (cartItem != null) {
                                    cartItem.setDocumentId(ds.getId());
                                    cartItems.add(cartItem);
                                }
                            }

                            if (binding == null) return;
                            binding.cartEmpty.setVisibility(View.GONE);
                            binding.cartCartItems.setVisibility(View.VISIBLE);

                            LinearLayoutManager layoutManager =
                                    new LinearLayoutManager(getContext());
                            binding.cartCartItems.setLayoutManager(layoutManager);

                            CartAdapter adapter = new CartAdapter(cartItems, getParentFragmentManager());
                            adapter.setOnQuantityChangeListener(cartItem -> {
                                if (binding == null) return;
                                String documentId = cartItem.getDocumentId();
                                db.collection("users").document(uid)
                                        .collection("cart")
                                        .document(documentId)
                                        .update("quantity", cartItem.getQuantity())
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(getContext(),
                                                        "Item quantity has been updated!",
                                                        Toast.LENGTH_SHORT).show());
                                updateTotal();
                            });

                            adapter.setOnRemoveListener(position -> {
                                if (binding == null) return;
                                String documentId = cartItems.get(position).getDocumentId();
                                db.collection("users").document(uid)
                                        .collection("cart")
                                        .document(documentId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            if (binding == null) return;
                                            cartItems.remove(position);
                                            adapter.notifyItemRemoved(position);
                                            adapter.notifyItemRangeChanged(
                                                    position, cartItems.size());
                                            updateTotal();
                                            if (getContext() == null) return;                                             Toast.makeText(getContext(),
                                                    "Item has been removed!",
                                                    Toast.LENGTH_SHORT).show();

                                            if (cartItems.isEmpty()) {
                                                binding.cartEmpty.setVisibility(View.VISIBLE);
                                                binding.cartCartItems.setVisibility(View.GONE);
                                            }
                                        });
                            });

                            binding.cartCartItems.setAdapter(adapter);
                            updateTotal();

                        } else {
                            if (binding == null) return;
                            binding.cartEmpty.setVisibility(View.VISIBLE);
                            binding.cartCartItems.setVisibility(View.GONE);
                        }
                    });
        }

        binding.cartBtnProceed.setOnClickListener(v -> {
            CheckoutFragment checkoutFragment = new CheckoutFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, checkoutFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void updateTotal() {
        if (binding == null || cartItems == null
                || cartItems.isEmpty()) {
            if (binding != null) {
                binding.cartTextTotal.setText(
                        String.format(Locale.US,
                                "LKR %,.2f", 0.00));
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> productIds = new ArrayList<>();
        cartItems.forEach(c -> productIds.add(c.getProductId()));

        db.collection("products")
                .whereIn("productId", productIds)
                .get()
                .addOnSuccessListener(qds -> {
                    Map<String, Product> productMap =
                            new HashMap<>();
                    qds.getDocuments().forEach(ds -> {
                        Product product =
                                ds.toObject(Product.class);
                        if (product != null)
                            productMap.put(
                                    product.getProductId(),
                                    product);
                    });

                    double total = 0;
                    for (CartItem cartItem : cartItems) {
                        Product product = productMap.get(
                                cartItem.getProductId());
                        if (product == null) continue;

                        int stock = product.getStockCount();

                        if (stock <= 0) continue;

                        int effectiveQty = Math.min(
                                cartItem.getQuantity(), stock);

                        total += product.getPrice()
                                * effectiveQty;
                    }

                    if (binding != null) {
                        binding.cartTextTotal.setText(
                                String.format(Locale.US,
                                        "LKR %,.2f", total));
                    }
                });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}