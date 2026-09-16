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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.zenova.gomart.R;
import lk.zenova.gomart.activity.SignInActivity;
import lk.zenova.gomart.adapter.ProductSliderAdapter;
import lk.zenova.gomart.adapter.SectionAdapter;
import lk.zenova.gomart.databinding.FragmentProductDetailsBinding;
import lk.zenova.gomart.model.Brand;
import lk.zenova.gomart.model.Product;
import lk.zenova.gomart.model.Wishlist;

public class ProductDetailsFragment extends Fragment {


    private FragmentProductDetailsBinding binding;
    private String productId;
    private int quantity = 1;
    private int avbQuantity;
    private boolean isWishlisted = false;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        getActivity().findViewById(R.id.bottom_navigation_view)
                .setVisibility(View.GONE);

        getActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });

        loadProduct();
        setupQuantityControls();
        setupButtons();
        loadTopSellProducts();
    }

    private void loadProduct() {
        db.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(ds -> {
                    if (binding == null) return;

                    if (ds.exists()) {
                        Product product = ds.toObject(Product.class);
                        bindProduct(product);
                    } else {
                        db.collection("products")
                                .whereEqualTo("productId", productId)
                                .get()
                                .addOnSuccessListener(qds -> {
                                    if (binding == null || qds.isEmpty()) return;
                                    Product product = qds.getDocuments()
                                            .get(0).toObject(Product.class);
                                    String desc = qds.getDocuments().get(0)
                                            .getString("description");
                                    if (product != null && desc != null) {
                                        product = new Product(
                                                product.getProductId(),
                                                product.getTitle(),
                                                desc,
                                                product.getPrice(),
                                                product.getCategoryId(),
                                                product.getImages(),
                                                product.getStockCount(),
                                                product.isStatus(),
                                                product.getRating(),
                                                product.getBrandId()
                                        );
                                    }
                                    bindProduct(product);
                                })
                                .addOnFailureListener(e -> showNoInternet());
                    }
                })
                .addOnFailureListener(e -> showNoInternet());
    }


    private void showNoInternet() {
        if (getActivity() == null || binding == null) return;

        Toast.makeText(getContext(),
                "No internet connection", Toast.LENGTH_SHORT).show();

        NoInternetFragment noInternet = new NoInternetFragment();

        noInternet.setOnRetryListener(() -> {
            ProductDetailsFragment f = new ProductDetailsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("productId", productId);
            f.setArguments(bundle);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .addToBackStack(null)
                    .commit();
        });

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, noInternet)
                .addToBackStack(null)
                .commit();
    }

    private void bindProduct(Product product) {
        if (product == null || binding == null) return;

        if (product.getImages() != null
                && !product.getImages().isEmpty()) {
            ProductSliderAdapter sliderAdapter =
                    new ProductSliderAdapter(product.getImages());
            binding.productImageSlider.setAdapter(sliderAdapter);
            binding.dotsIndicator.attachTo(
                    binding.productImageSlider);
        }

        binding.productDetailsTitle.setText(product.getTitle());
        binding.productDetailsRating.setRating(product.getRating());
        binding.productDetailsRatingText.setText(
                String.valueOf(product.getRating()));
        binding.productDetailsPrice.setText(
                String.format("LKR %,.2f", product.getPrice()));

        avbQuantity = product.getStockCount();
        binding.productDetailsAvbQty.setText(
                String.valueOf(avbQuantity));

        if (avbQuantity <= 0) {
            binding.productDetailsBtnAddCart.setEnabled(false);
            binding.productDetailsBtnAddCart.setAlpha(0.4f);
            binding.productDetailsBtnAddCart.setText("Out of Stock");
            binding.productDetailsBtnBuyNow.setEnabled(false);
            binding.productDetailsBtnBuyNow.setAlpha(0.4f);
            binding.productDetailsBtnMinus.setEnabled(false);
            binding.productDetailsBtnPlus.setEnabled(false);
        } else {
            binding.productDetailsBtnAddCart.setEnabled(true);
            binding.productDetailsBtnAddCart.setAlpha(1.0f);
            binding.productDetailsBtnAddCart.setText("Add to Cart");
            binding.productDetailsBtnBuyNow.setEnabled(true);
            binding.productDetailsBtnBuyNow.setAlpha(1.0f);
            binding.productDetailsBtnMinus.setEnabled(true);
            binding.productDetailsBtnPlus.setEnabled(true);
        }

        String desc = product.getDescription();
        if (desc != null && !desc.isEmpty()) {
            binding.productDetailsDescription.setText(desc);
        } else {
            binding.productDetailsDescription.setText(
                    "No description available.");
        }

        if (product.getBrandId() != null
                && !product.getBrandId().isEmpty()) {
            db.collection("brands")
                    .whereEqualTo("brandId", product.getBrandId())
                    .get()
                    .addOnSuccessListener(brandSnap -> {
                        if (binding == null
                                || brandSnap.isEmpty()) return;
                        Brand brand = brandSnap.getDocuments()
                                .get(0).toObject(Brand.class);
                        if (brand != null)
                            binding.productDetailsBrandName
                                    .setText(brand.getBrandName());
                    })
                    .addOnFailureListener(e ->
                            binding.productDetailsBrandName
                                    .setText("Unknown Brand"));
        } else {
            binding.productDetailsBrandName.setText("No Brand");
        }

        if (firebaseAuth.getCurrentUser() != null) {
            String uid = firebaseAuth.getCurrentUser().getUid();
            db.collection("users").document(uid)
                    .collection("wishlist")
                    .whereEqualTo("productId", productId)
                    .get()
                    .addOnSuccessListener(wSnap -> {
                        if (binding == null) return;
                        isWishlisted = !wSnap.isEmpty();
                        updateWishlistIcon();
                    });
        }
    }

    private void setupQuantityControls() {
        binding.productDetailsBtnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnPlus.setOnClickListener(v -> {
            if (quantity < avbQuantity) {
                quantity++;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });
    }


    private void setupButtons() {

        binding.productDetailsBtnWishlist.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                return;
            }
            String uid = firebaseAuth.getCurrentUser().getUid();

            if (isWishlisted) {
                db.collection("users").document(uid)
                        .collection("wishlist")
                        .whereEqualTo("productId", productId)
                        .get()
                        .addOnSuccessListener(wSnap -> {
                            wSnap.getDocuments().forEach(
                                    d -> d.getReference().delete());
                            isWishlisted = false;
                            updateWishlistIcon();
                            Toast.makeText(getContext(),
                                    "Removed from wishlist",
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                Wishlist wishlistItem = new Wishlist(productId, 1, null);
                db.collection("users").document(uid)
                        .collection("wishlist").document()
                        .set(wishlistItem)
                        .addOnSuccessListener(unused -> {
                            isWishlisted = true;
                            updateWishlistIcon();
                            Toast.makeText(getContext(),
                                    "Added to wishlist!",
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });

        binding.productDetailsBtnAddCart.setOnClickListener(v -> {

            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                return;
            }

            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users").document(uid)
                    .collection("cart")
                    .document(productId)
                    .get()
                    .addOnSuccessListener(doc -> {

                        int currentQty = 0;

                        if (doc.exists()) {
                            Long qty = doc.getLong("quantity");
                            currentQty = qty != null ? qty.intValue() : 0;
                        }

                        int newQty = currentQty + quantity;

                        Map<String, Object> cartItem = new HashMap<>();
                        cartItem.put("productId", productId);
                        cartItem.put("quantity", newQty);

                        db.collection("users").document(uid)
                                .collection("cart")
                                .document(productId)
                                .set(cartItem)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(getContext(),
                                                "Added to cart! (Qty: " + newQty + ")",
                                                Toast.LENGTH_SHORT).show());
                    });
        });

        binding.productDetailsBtnBuyNow.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                return;
            }
            Bundle args = new Bundle();
            args.putString(CheckoutFragment.ARG_BUY_NOW_PRODUCT_ID, productId);
            args.putInt(CheckoutFragment.ARG_BUY_NOW_QUANTITY, quantity);
            CheckoutFragment checkout = new CheckoutFragment();
            checkout.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, checkout)
                    .addToBackStack(null)
                    .commit();
        });
    }


    private void updateWishlistIcon() {
        if (binding == null) return;
        binding.productDetailsBtnWishlist.setImageResource(
                isWishlisted
                        ? R.drawable.favorite_24
                        : R.drawable.favorite);
    }


    private void addToCart(Product product) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), SignInActivity.class));
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        String imageUrl = (product.getImages() != null && !product.getImages().isEmpty())
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
                                "Max stock reached! Only " + product.getStockCount() + " available.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newQty = currentQty + 1;

                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productId", product.getProductId());
                    cartItem.put("title", product.getTitle());
                    cartItem.put("price", product.getPrice());
                    cartItem.put("imageUrl", imageUrl);
                    cartItem.put("quantity", newQty);

                    db.collection("users").document(uid)
                            .collection("cart")
                            .document(product.getProductId())
                            .set(cartItem)
                            .addOnSuccessListener(unused -> {
                                if (isAdded()) {
                                    Toast.makeText(getContext(),
                                            product.getTitle() + " added! (Qty: " + newQty + ")",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to add to cart", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void loadTopSellProducts() {
        db.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(ds -> {
                    if (binding == null || !ds.exists()) return;

                    Product currentProduct = ds.toObject(Product.class);
                    if (currentProduct == null
                            || currentProduct.getCategoryId() == null
                            || currentProduct.getCategoryId().isEmpty()) return;

                    String categoryId = currentProduct.getCategoryId();

                    db.collection("products")
                            .whereEqualTo("categoryId", categoryId)
                            .get()
                            .addOnSuccessListener(qds -> {
                                if (binding == null || qds.isEmpty()) return;

                                List<Product> products = qds.toObjects(Product.class);
                                products.removeIf(p ->
                                        productId.equals(p.getProductId()));

                                if (products.isEmpty()) {
                                    binding.productDetailsTopSellSection
                                            .getRoot().setVisibility(View.GONE);
                                    return;
                                }

                                LinearLayoutManager layoutManager =
                                        new LinearLayoutManager(getContext(),
                                                LinearLayoutManager.HORIZONTAL, false);
                                binding.productDetailsTopSellSection
                                        .itemSectionContainer
                                        .setLayoutManager(layoutManager);

                                SectionAdapter adapter = new SectionAdapter(
                                        products,
                                        product -> {
                                            Bundle bundle = new Bundle();
                                            bundle.putString("productId",
                                                    product.getProductId());
                                            ProductDetailsFragment f =
                                                    new ProductDetailsFragment();
                                            f.setArguments(bundle);
                                            getParentFragmentManager()
                                                    .beginTransaction()
                                                    .replace(R.id.fragment_container, f)
                                                    .addToBackStack(null)
                                                    .commit();
                                        },
                                        product -> addToCart(product));

                                binding.productDetailsTopSellSection
                                        .itemSectionTitle.setText("Similar Products");
                                binding.productDetailsTopSellSection
                                        .itemSectionContainer.setAdapter(adapter);
                            })
                            .addOnFailureListener(e ->
                                    binding.productDetailsTopSellSection
                                            .getRoot().setVisibility(View.GONE));
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