package lk.zenova.gomart.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.adapter.BrandAdapter;
import lk.zenova.gomart.adapter.SearchResultAdapter;
import lk.zenova.gomart.adapter.SearchSuggestionAdapter;
import lk.zenova.gomart.adapter.SectionAdapter;
import lk.zenova.gomart.databinding.FragmentHomeBinding;
import lk.zenova.gomart.model.Brand;
import lk.zenova.gomart.model.Category;
import lk.zenova.gomart.model.Product;
import lk.zenova.gomart.model.SearchResultItem;
import lk.zenova.gomart.model.SuggestionItem;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore   db;

    private List<Product>  allProducts   = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private List<Brand>    allBrands     = new ArrayList<>();
    private boolean dataLoaded = false;

    private SearchResultAdapter     searchAdapter;
    private SearchSuggestionAdapter suggestionAdapter;

    private static final String MUNCHEE_BRAND_ID =
            "2yXmR7G03Hz9G2Nxaq8Y";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(
                inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        setupSearchRecycler();
        setupSearchBar();
        setupAutoComplete();
        loadTopSellProducts();
        loadOffersProducts();
        loadBrands();
        preloadSearchData();
    }



    private void setupSearchBar() {

        binding.textInputSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override public void beforeTextChanged(
                            CharSequence s, int st, int c, int a) {}
                    @Override public void onTextChanged(
                            CharSequence s, int st, int b, int c) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String query = s.toString().trim();

                        if (query.isEmpty()) {
                            binding.homeSearchOverlay
                                    .setVisibility(View.GONE);
                            binding.homeScrollContent
                                    .setVisibility(View.VISIBLE);
                            binding.textInputSearch
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            R.drawable.search_20,
                                            0, 0, 0);
                        } else {
                            binding.homeSearchOverlay
                                    .setVisibility(View.VISIBLE);
                            binding.homeScrollContent
                                    .setVisibility(View.GONE);
                            performSearch(query.toLowerCase());
                            binding.textInputSearch
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            R.drawable.search_20,
                                            0, R.drawable.close_24, 0);
                        }
                    }
                });

        binding.textInputSearch.setOnTouchListener(
                (v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (binding.textInputSearch
                                .getCompoundDrawables()[2] != null) {
                            int drawableRight =
                                    binding.textInputSearch.getRight()
                                            - binding.textInputSearch
                                            .getCompoundDrawables()[2]
                                            .getBounds().width()
                                            - binding.textInputSearch
                                            .getPaddingEnd();
                            if (event.getRawX() >= drawableRight) {
                                clearSearch();
                                return true;
                            }
                        }
                    }
                    return false;
                });

        binding.textInputSearch.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String q = binding.textInputSearch
                                .getText().toString().trim();
                        if (!q.isEmpty())
                            performSearch(q.toLowerCase());
                        return true;
                    }
                    return false;
                });
    }

    private void setupAutoComplete() {
        suggestionAdapter = new SearchSuggestionAdapter(
                requireContext(), new ArrayList<>());
        binding.textInputSearch.setAdapter(suggestionAdapter);
        binding.textInputSearch.setThreshold(1);

        binding.textInputSearch.setOnItemClickListener(
                (parent, view, position, id) -> {
                    SuggestionItem selected =
                            suggestionAdapter.getItem(position);
                    if (selected == null) return;

                    binding.textInputSearch
                            .setText(selected.getDisplayName());
                    binding.textInputSearch.dismissDropDown();

                    switch (selected.getType()) {
                        case SuggestionItem.TYPE_PRODUCT:
                            Product p = (Product) selected.getData();
                            navigateToProduct(p.getProductId());
                            break;
                        case SuggestionItem.TYPE_BRAND:
                            Brand b = (Brand) selected.getData();
                            navigateToBrand(
                                    b.getBrandId(), b.getBrandName());
                            break;
                        case SuggestionItem.TYPE_CATEGORY:
                            Category c = (Category) selected.getData();
                            Bundle bundle = new Bundle();
                            bundle.putString("categoryId",
                                    c.getCategoryId());
                            bundle.putString("categoryName",
                                    c.getName());
                            ListingFragment lf = new ListingFragment();
                            lf.setArguments(bundle);
                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, lf)
                                    .addToBackStack(null)
                                    .commit();
                            break;
                    }
                });
    }

    private void setupSearchRecycler() {
        searchAdapter = new SearchResultAdapter(
                new ArrayList<>(),
                new SearchResultAdapter
                        .OnSearchResultClickListener() {

                    @Override
                    public void onProductClick(Product product) {
                        clearSearch();
                        navigateToProduct(
                                product.getProductId());
                    }

                    @Override
                    public void onCategoryClick(
                            Category category) {
                        clearSearch();
                        Bundle bundle = new Bundle();
                        bundle.putString("categoryId",
                                category.getCategoryId());
                        bundle.putString("categoryName",
                                category.getName());
                        ListingFragment f =
                                new ListingFragment();
                        f.setArguments(bundle);
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(
                                        R.id.fragment_container,
                                        f)
                                .addToBackStack(null)
                                .commit();
                    }

                    @Override
                    public void onBrandClick(Brand brand) {
                        clearSearch();
                        navigateToBrand(
                                brand.getBrandId(),
                                brand.getBrandName());
                    }
                });

        binding.searchResultsRecycler.setLayoutManager(
                new LinearLayoutManager(getContext()));
        binding.searchResultsRecycler.setAdapter(
                searchAdapter);
    }

    private void performSearch(String query) {
        if (!dataLoaded) {
            binding.searchProgress.setVisibility(View.VISIBLE);
            binding.searchEmptyText.setVisibility(View.GONE);
            return;
        }

        binding.searchProgress.setVisibility(View.GONE);
        List<SearchResultItem> results = new ArrayList<>();

        List<SearchResultItem> productResults =
                new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getTitle() != null
                    && p.getTitle().toLowerCase()
                    .contains(query)) {
                productResults.add(new SearchResultItem(
                        SearchResultItem.TYPE_PRODUCT, p));
            }
        }
        if (!productResults.isEmpty()) {
            results.add(new SearchResultItem("PRODUCTS"));
            results.addAll(productResults);
        }

        List<SearchResultItem> categoryResults =
                new ArrayList<>();
        for (Category c : allCategories) {
            if (c.getName() != null
                    && c.getName().toLowerCase()
                    .contains(query)) {
                categoryResults.add(new SearchResultItem(
                        SearchResultItem.TYPE_CATEGORY, c));
            }
        }
        if (!categoryResults.isEmpty()) {
            results.add(new SearchResultItem("CATEGORIES"));
            results.addAll(categoryResults);
        }

        List<SearchResultItem> brandResults =
                new ArrayList<>();
        for (Brand b : allBrands) {
            if (b.getBrandName() != null
                    && b.getBrandName().toLowerCase()
                    .contains(query)) {
                brandResults.add(new SearchResultItem(
                        SearchResultItem.TYPE_BRAND, b));
            }
        }
        if (!brandResults.isEmpty()) {
            results.add(new SearchResultItem("BRANDS"));
            results.addAll(brandResults);
        }

        if (results.isEmpty()) {
            binding.searchEmptyText.setVisibility(View.VISIBLE);
            binding.searchResultsRecycler
                    .setVisibility(View.GONE);
        } else {
            binding.searchEmptyText.setVisibility(View.GONE);
            binding.searchResultsRecycler
                    .setVisibility(View.VISIBLE);
            searchAdapter.updateItems(results);
        }
    }

    private void clearSearch() {
        if (binding == null) return;
        binding.textInputSearch.setText("");
        binding.homeSearchOverlay.setVisibility(View.GONE);
        binding.homeScrollContent.setVisibility(View.VISIBLE);
        binding.textInputSearch
                .setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.search_20, 0, 0, 0);
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(
                    binding.textInputSearch
                            .getWindowToken(), 0);
        }
    }



    private void preloadSearchData() {
        db.collection("products").get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    allProducts = qds.toObjects(Product.class);
                    checkDataLoaded();
                });
        db.collection("categories").get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    allCategories = qds.toObjects(Category.class);
                    checkDataLoaded();
                });
        db.collection("brands").get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    allBrands = qds.toObjects(Brand.class);
                    checkDataLoaded();
                });
    }

    private void checkDataLoaded() {
        if (!allProducts.isEmpty()
                || !allCategories.isEmpty()
                || !allBrands.isEmpty()) {
            dataLoaded = true;
            if (binding != null)
                binding.searchProgress
                        .setVisibility(View.GONE);
            buildSuggestions();
        }
    }

    private void buildSuggestions() {
        List<SuggestionItem> suggestions = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getTitle() != null
                    && !p.getTitle().isEmpty())
                suggestions.add(new SuggestionItem(
                        p.getTitle(),
                        SuggestionItem.TYPE_PRODUCT, p));
        }
        for (Category c : allCategories) {
            if (c.getName() != null
                    && !c.getName().isEmpty())
                suggestions.add(new SuggestionItem(
                        c.getName(),
                        SuggestionItem.TYPE_CATEGORY, c));
        }
        for (Brand b : allBrands) {
            if (b.getBrandName() != null
                    && !b.getBrandName().isEmpty())
                suggestions.add(new SuggestionItem(
                        b.getBrandName(),
                        SuggestionItem.TYPE_BRAND, b));
        }
        if (suggestionAdapter != null)
            suggestionAdapter.updateAllItems(suggestions);
    }



    private void loadTopSellProducts() {
        db.collection("products")
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    if (qds.isEmpty()) return;

                    List<Product> products =
                            qds.toObjects(Product.class);

                    LinearLayoutManager lm =
                            new LinearLayoutManager(
                                    getContext(),
                                    LinearLayoutManager.HORIZONTAL,
                                    false);
                    binding.homeTopSellSection
                            .itemSectionContainer
                            .setLayoutManager(lm);

                    SectionAdapter adapter =
                            new SectionAdapter(
                                    products,
                                    product -> navigateToProduct(
                                            product.getProductId()),
                                    product -> addToCart(product));

                    binding.homeTopSellSection
                            .itemSectionTitle
                            .setText("Best Selling");
                    binding.homeTopSellSection
                            .itemSectionSeeMore
                            .setVisibility(View.VISIBLE);
                    binding.homeTopSellSection
                            .itemSectionSeeMore
                            .setOnClickListener(v ->
                                    navigateToAllProducts());
                    binding.homeTopSellSection
                            .itemSectionContainer
                            .setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("HOME",
                                "Best selling error: "
                                        + e.getMessage()));
    }

    private void loadOffersProducts() {
        db.collection("products")
                .whereEqualTo("brandId", MUNCHEE_BRAND_ID)
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    if (qds.isEmpty()) {
                        binding.homeOfferSection.getRoot()
                                .setVisibility(View.GONE);
                        return;
                    }

                    List<Product> products =
                            qds.toObjects(Product.class);

                    LinearLayoutManager lm =
                            new LinearLayoutManager(
                                    getContext(),
                                    LinearLayoutManager.HORIZONTAL,
                                    false);
                    binding.homeOfferSection
                            .itemSectionContainer
                            .setLayoutManager(lm);

                    SectionAdapter adapter =
                            new SectionAdapter(
                                    products,
                                    product -> navigateToProduct(
                                            product.getProductId()),
                                    product -> addToCart(product));

                    binding.homeOfferSection
                            .itemSectionTitle.setText("Offers");
                    binding.homeOfferSection
                            .itemSectionSeeMore
                            .setVisibility(View.VISIBLE);
                    binding.homeOfferSection
                            .itemSectionSeeMore
                            .setOnClickListener(v ->
                                    navigateToBrand(
                                            MUNCHEE_BRAND_ID,
                                            "Munchee"));
                    binding.homeOfferSection
                            .itemSectionContainer
                            .setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("HOME",
                                "Offers error: "
                                        + e.getMessage()));
    }

    private void loadBrands() {
        db.collection("brands")
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    if (qds.isEmpty()) return;

                    List<Brand> brands =
                            qds.toObjects(Brand.class);

                    LinearLayoutManager lm =
                            new LinearLayoutManager(
                                    getContext(),
                                    LinearLayoutManager.HORIZONTAL,
                                    false);
                    binding.homeBrandSection
                            .itemSectionContainer
                            .setLayoutManager(lm);

                    BrandAdapter adapter = new BrandAdapter(
                            brands, brand -> {
                        if (brand == null
                                || brand.getBrandId() == null)
                            return;
                        navigateToBrand(
                                brand.getBrandId(),
                                brand.getBrandName());
                    });

                    binding.homeBrandSection
                            .itemSectionTitle
                            .setText("Shop by Brand");
                    binding.homeBrandSection
                            .itemSectionSeeMore
                            .setVisibility(View.GONE);
                    binding.homeBrandSection
                            .itemSectionContainer
                            .setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("HOME",
                                "Brands error: "
                                        + e.getMessage()));
    }



    private void navigateToProduct(String productId) {
        Bundle bundle = new Bundle();
        bundle.putString("productId", productId);
        ProductDetailsFragment f =
                new ProductDetailsFragment();
        f.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToBrand(String brandId,
                                 String brandName) {
        Bundle bundle = new Bundle();
        bundle.putString("brandId",   brandId);
        bundle.putString("brandName", brandName);
        BrandListingFragment f = new BrandListingFragment();
        f.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToAllProducts() {
        AllProductsFragment f = new AllProductsFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }



    private void addToCart(Product product) {
        String userId = FirebaseAuth.getInstance()
                .getCurrentUser() != null
                ? FirebaseAuth.getInstance()
                .getCurrentUser().getUid()
                : null;

        if (userId == null) {
            startActivity(new Intent(getActivity(),
                    lk.zenova.gomart.activity
                            .SignInActivity.class));
            return;
        }

        if (product.getProductId() == null
                || product.getProductId().isEmpty()) return;

        String imageUrl = (product.getImages() != null
                && !product.getImages().isEmpty())
                ? product.getImages().get(0) : "";

        db.collection("users").document(userId)
                .collection("cart")
                .document(product.getProductId())
                .get()
                .addOnSuccessListener(doc -> {
                    int currentQty = 0;
                    if (doc.exists()) {
                        Long qty = doc.getLong("quantity");
                        currentQty = qty != null
                                ? qty.intValue() : 0;
                    }

                    if (currentQty >= product.getStockCount()) {
                        Toast.makeText(getContext(),
                                "Max stock reached! Only "
                                        + product.getStockCount()
                                        + " available.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newQty = currentQty + 1;

                    java.util.Map<String, Object> cartItem =
                            new java.util.HashMap<>();
                    cartItem.put("productId",
                            product.getProductId());
                    cartItem.put("title",  product.getTitle());
                    cartItem.put("price",  product.getPrice());
                    cartItem.put("imageUrl", imageUrl);
                    cartItem.put("quantity", newQty);

                    db.collection("users").document(userId)
                            .collection("cart")
                            .document(product.getProductId())
                            .set(cartItem)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(getContext(),
                                                    product.getTitle()
                                                            + " added! (Qty: "
                                                            + newQty + ")",
                                                    Toast.LENGTH_SHORT)
                                            .show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                                    "Failed to add",
                                                    Toast.LENGTH_SHORT)
                                            .show());
                });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}