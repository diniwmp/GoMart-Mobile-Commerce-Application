package lk.zenova.gomart.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import lk.zenova.gomart.R;
import lk.zenova.gomart.activity.InvoiceActivity;
import lk.zenova.gomart.databinding.FragmentCheckoutBinding;
import lk.zenova.gomart.helper.NotificationHelper;
import lk.zenova.gomart.listener.FirestoreCallback;
import lk.zenova.gomart.model.Address;
import lk.zenova.gomart.model.CartItem;
import lk.zenova.gomart.model.Order;
import lk.zenova.gomart.model.Product;
import lk.zenova.gomart.model.User;

public class CheckoutFragment extends Fragment {

    public static final String ARG_BUY_NOW_PRODUCT_ID = "buy_now_product_id";
    public static final String ARG_BUY_NOW_QUANTITY = "buy_now_quantity";

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private double total;
    private boolean paymentActive;

    private boolean isBuyNow = false;
    private String buyNowProductId;
    private int buyNowQuantity;

    private Address selectedAddress = null;
    private List<Address> savedAddresses = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (getArguments() != null
                && getArguments().containsKey(ARG_BUY_NOW_PRODUCT_ID)) {
            isBuyNow = true;
            buyNowProductId = getArguments().getString(ARG_BUY_NOW_PRODUCT_ID);
            buyNowQuantity = getArguments().getInt(ARG_BUY_NOW_QUANTITY, 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);

        binding.shippingLayoutBtn.setOnClickListener(v -> {
            if (binding.shippingLayoutBody.getVisibility() == View.GONE) {
                binding.shippingLayoutBody.setVisibility(View.VISIBLE);
                binding.shippingLayoutBtn.setRotation(180f);
            } else {
                binding.shippingLayoutBody.setVisibility(View.GONE);
                binding.shippingLayoutBtn.setRotation(0f);
            }
        });

        binding.billingLayoutBtn.setOnClickListener(v -> {
            if (binding.billingLayoutBody.getVisibility() == View.GONE) {
                binding.billingLayoutBody.setVisibility(View.VISIBLE);
                binding.billingLayoutBtn.setRotation(180f);
            } else {
                binding.billingLayoutBody.setVisibility(View.GONE);
                binding.billingLayoutBtn.setRotation(0f);
            }
        });

        binding.shippingDetailsCheckBilling.setOnCheckedChangeListener(
                (btn, isChecked) -> binding.billingLayout.setVisibility(
                        isChecked ? View.GONE : View.VISIBLE));

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserInfo();
        loadSavedAddresses();

        double shippingCost = 300;

        if (isBuyNow) {
            loadBuyNowTotal(shippingCost);
        } else {
            loadCartTotal(shippingCost);
        }

        binding.checkoutBtnProceed.setOnClickListener(v -> {
            if (!paymentActive) return;
            proceedToPayment();
        });
    }

    private void loadBuyNowTotal(double shippingCost) {
        db.collection("products")
                .document(buyNowProductId)
                .get()
                .addOnSuccessListener(ds -> {
                    if (binding == null || !ds.exists()) return;
                    Product product = ds.toObject(Product.class);
                    if (product == null) return;

                    double subTotal = product.getPrice() * buyNowQuantity;
                    total = subTotal + shippingCost;

                    binding.checkoutSubtotal.setText(
                            String.format(Locale.US, "LKR %,.2f", subTotal));
                    binding.checkoutShipping.setText(
                            String.format(Locale.US, "LKR %,.2f", shippingCost));
                    binding.checkoutTotal.setText(
                            String.format(Locale.US, "LKR %,.2f", total));
                    paymentActive = true;
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load product",
                                Toast.LENGTH_SHORT).show());
    }

    private void loadCartTotal(double shippingCost) {
        getCartItems(cartItems -> {
            ArrayList<String> productIds = new ArrayList<>();
            cartItems.forEach(ci -> productIds.add(ci.getProductId()));

            getProductsByIds(productIds, data -> {
                double subTotal = 0;

                for (CartItem ci : cartItems) {
                    Product p = data.get(ci.getProductId());
                    if (p == null) continue;

                    int stock = p.getStockCount();

                    if (stock <= 0) continue;

                    int effectiveQty = Math.min(
                            ci.getQuantity(), stock);

                    subTotal += p.getPrice() * effectiveQty;
                }

                total = subTotal + shippingCost;

                if (binding == null) return;
                binding.checkoutSubtotal.setText(
                        String.format(Locale.US,
                                "LKR %,.2f", subTotal));
                binding.checkoutShipping.setText(
                        String.format(Locale.US,
                                "LKR %,.2f", shippingCost));
                binding.checkoutTotal.setText(
                        String.format(Locale.US,
                                "LKR %,.2f", total));
                paymentActive = true;
            });
        });
    }

    private void proceedToPayment() {
        String name = binding.shippingDetailsName
                .getText().toString().trim();
        String email = binding.shippingDetailsEmail
                .getText().toString().trim();
        String contact = binding.shippingDetailsContact
                .getText().toString().trim();

        // Check if billing is different from shipping
        boolean billingDifferent =
                !binding.shippingDetailsCheckBilling.isChecked();

        // Get delivery address
        // from saved card or manual shipping field
        String fullAddress = selectedAddress != null
                ? selectedAddress.getAddress()
                : binding.shippingDetailsAddress
                .getText().toString().trim();

        // Validate sender details (always required)
        if (name.isEmpty()) {
            Toast.makeText(getContext(),
                    "Please enter your name",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(getContext(),
                    "Please enter your email",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (contact.isEmpty()) {
            Toast.makeText(getContext(),
                    "Please enter your contact number",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (billingDifferent) {

            String billingName = binding.billingDetailsName
                    .getText().toString().trim();
            String billingEmail = binding.billingDetailsEmail
                    .getText().toString().trim();
            String billingContact = binding.billingDetailsContact
                    .getText().toString().trim();
            String billingAddress = binding.billingDetailsAddress
                    .getText().toString().trim();

            if (billingName.isEmpty()) {
                Toast.makeText(getContext(),
                        "Please enter recipient name",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (billingEmail.isEmpty()) {
                Toast.makeText(getContext(),
                        "Please enter recipient email",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (billingContact.isEmpty()) {
                Toast.makeText(getContext(),
                        "Please enter recipient contact",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (billingAddress.isEmpty()) {
                Toast.makeText(getContext(),
                        "Please enter recipient address",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // for PayHere
            fullAddress = billingAddress;

        } else {

            if (fullAddress.isEmpty()) {
                Toast.makeText(getContext(),
                        "Please select or enter a delivery address",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId("1224059");
        req.setMerchantSecret(
                "MzA1NjI3ODc4NDMxNzA4MTk3MzAxOTcwMjc3ODUyNTk3NjQ5MDA=");
        req.setCurrency("LKR");
        req.setAmount(total);
        req.setOrderId("GOMART-" + System.currentTimeMillis());
        req.setItemsDescription(
                isBuyNow ? "GoMart Buy Now" : "GoMart Order");

        req.getCustomer().setFirstName(name);
        req.getCustomer().setLastName("");
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(contact);
        req.getCustomer().getAddress().setAddress(fullAddress);
        req.getCustomer().getAddress().setCity("");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(getActivity(),
                PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        payhereLauncher.launch(intent);
    }



    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().hasExtra(
                                PHConstants.INTENT_EXTRA_RESULT)) {
                            PHResponse<StatusResponse> response =
                                    (PHResponse<StatusResponse>)
                                            result.getData().getSerializableExtra(
                                                    PHConstants.INTENT_EXTRA_RESULT);
                            if (response != null && response.isSuccess()) {
                                saveOrder(response.getData());
                            }
                        }
                    });


    private void saveOrder(StatusResponse statusResponse) {
        if (binding == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        String name = binding.shippingDetailsName
                .getText().toString().trim();
        String email = binding.shippingDetailsEmail
                .getText().toString().trim();
        String contact = binding.shippingDetailsContact
                .getText().toString().trim();
        String addressNickname = selectedAddress != null
                ? selectedAddress.getAddressName()
                : binding.shippingDetailsAddressName
                .getText().toString().trim();
        String fullAddress = selectedAddress != null
                ? selectedAddress.getAddress()
                : binding.shippingDetailsAddress
                .getText().toString().trim();

        Order.Address shipping = Order.Address.builder()
                .name(name).email(email).contact(contact)
                .addressName(addressNickname)
                .address(fullAddress)
                .build();

        Order.Address billing = null;
        if (!binding.shippingDetailsCheckBilling.isChecked()) {
            billing = Order.Address.builder()
                    .name(binding.billingDetailsName
                            .getText().toString().trim())
                    .email(binding.billingDetailsEmail
                            .getText().toString().trim())
                    .contact(binding.billingDetailsContact
                            .getText().toString().trim())
                    .addressName("")
                    .address(binding.billingDetailsAddress
                            .getText().toString().trim())
                    .build();
        }
        Order.Address finalBilling = billing;

        if (isBuyNow) {
            db.collection("products")
                    .document(buyNowProductId).get()
                    .addOnSuccessListener(ds -> {
                        if (!ds.exists()) return;
                        Product p = ds.toObject(Product.class);
                        if (p == null) return;

                        // Check stock for buy now product
                        if (p.getStockCount() <= 0) {
                            Toast.makeText(getContext(),
                                    "This product is out of stock",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Order.OrderItem item =
                                Order.OrderItem.builder()
                                        .productId(buyNowProductId)
                                        .unitPrice(p.getPrice())
                                        .quantity(buyNowQuantity)
                                        .attributes(new ArrayList<>())
                                        .build();

                        persistOrder(uid, shipping, finalBilling,
                                Collections.singletonList(item),
                                false);
                    });
        } else {
            getCartItems(cartItems -> {
                ArrayList<String> ids = new ArrayList<>();
                cartItems.forEach(ci ->
                        ids.add(ci.getProductId()));

                getProductsByIds(ids, data -> {
                    List<Order.OrderItem> items = new ArrayList<>();
                    double recalcSubtotal = 0;

                    for (CartItem ci : cartItems) {
                        Product p = data.get(ci.getProductId());
                        if (p == null) continue;

                        // Skip out of stock products
                        if (p.getStockCount() <= 0) continue;

                        // Use capped quantity
                        int effectiveQty = Math.min(
                                ci.getQuantity(),
                                p.getStockCount());

                        items.add(Order.OrderItem.builder()
                                .productId(ci.getProductId())
                                .unitPrice(p.getPrice())
                                .quantity(effectiveQty)
                                .attributes(new ArrayList<>())
                                .build());

                        recalcSubtotal +=
                                p.getPrice() * effectiveQty;
                    }


                    total = recalcSubtotal + 300;

                    persistOrder(uid, shipping, finalBilling,
                            items, true);
                });
            });
        }
    }


    private void persistOrder(String uid,
                              Order.Address shipping,
                              Order.Address billing,
                              List<Order.OrderItem> orderItems,
                              boolean clearCart) {
        Order order = new Order();
        order.setOrderId(String.valueOf(System.currentTimeMillis()));
        order.setUserId(uid);
        order.setTotalAmount(total);
        order.setStatus("PAID");
        order.setOrderDate(Timestamp.now());
        order.setShippingAddress(shipping);
        order.setOrderItems(orderItems);
        if (billing != null) order.setBillingAddress(billing);

        db.collection("orders").document().set(order)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            "Order placed successfully!",
                            Toast.LENGTH_SHORT).show();

                    NotificationHelper.notifyOrderPlaced(
                            getContext(), order.getOrderId());
                    NotificationHelper.notifyPaymentConfirmed(
                            getContext(), order.getOrderId(), total);

                    for (Order.OrderItem item : orderItems) {
                        db.collection("products")
                                .whereEqualTo("productId",
                                        item.getProductId())
                                .get()
                                .addOnSuccessListener(qds -> {
                                    if (qds.isEmpty()) return;

                                    String docId = qds.getDocuments()
                                            .get(0).getId();

                                    Long stockVal = qds.getDocuments()
                                            .get(0).getLong("stockCount");

                                    final int currentStock =
                                            stockVal != null
                                                    ? stockVal.intValue() : 0;

                                    int newStock = Math.max(
                                            0,
                                            currentStock - item.getQuantity());

                                    db.collection("products")
                                            .document(docId)
                                            .update("stockCount", newStock)
                                            .addOnSuccessListener(u ->
                                                    Log.d("STOCK",
                                                            item.getProductId()
                                                                    + " stock: "
                                                                    + currentStock
                                                                    + " → " + newStock))
                                            .addOnFailureListener(e ->
                                                    Log.e("STOCK",
                                                            "Failed: "
                                                                    + e.getMessage()));
                                });
                    }

// Clear ONLY purchased items from cart
                    if (clearCart) {
                        List<String> purchasedIds = new ArrayList<>();
                        for (Order.OrderItem item : orderItems) {
                            purchasedIds.add(item.getProductId());
                        }

                        db.collection("users").document(uid)
                                .collection("cart").get()
                                .addOnSuccessListener(qds ->
                                        qds.getDocuments().forEach(ds -> {

                                            String cartProductId =
                                                    ds.getString("productId");
                                            if (cartProductId != null
                                                    && purchasedIds.contains(
                                                    cartProductId)) {
                                                ds.getReference().delete();
                                            }
                                        }));
                    }


                    Intent intent = new Intent(
                            getActivity(), InvoiceActivity.class);
                    intent.putExtra("orderId", order.getOrderId());
                    requireActivity().getSupportFragmentManager()
                            .popBackStack(null,
                                    androidx.fragment.app.FragmentManager
                                            .POP_BACK_STACK_INCLUSIVE);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to save order",
                                Toast.LENGTH_SHORT).show());
    }


    private void loadUserInfo() {
        if (firebaseAuth.getCurrentUser() == null) return;
        db.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(ds -> {
                    if (binding == null || !ds.exists()) return;
                    User user = ds.toObject(User.class);
                    if (user != null) {
                        binding.shippingDetailsName.setText(user.getName());
                        binding.shippingDetailsEmail.setText(user.getEmail());
                        binding.shippingDetailsContact.setText(user.getMobile());
                    }
                });
    }

    private void loadSavedAddresses() {
        if (firebaseAuth.getCurrentUser() == null) return;
        db.collection("addresses")
                .whereEqualTo("userId",
                        firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;
                    if (qds.isEmpty()) {
                        binding.shippingNoAddressMsg.setVisibility(View.VISIBLE);
                        return;
                    }
                    savedAddresses.clear();
                    for (int i = 0; i < qds.getDocuments().size(); i++) {
                        Address a = qds.getDocuments().get(i)
                                .toObject(Address.class);
                        if (a != null) {
                            a.setDocumentId(qds.getDocuments().get(i).getId());
                            savedAddresses.add(a);
                        }
                    }
                    buildAddressCards();
                });
    }

    private void buildAddressCards() {
        android.widget.LinearLayout container = binding.shippingAddressList;
        container.removeAllViews();

        for (Address addr : savedAddresses) {
            View card = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_address, container, false);

            android.widget.TextView tvNickname =
                    card.findViewById(R.id.address_select_nickname);
            android.widget.TextView tvAddress =
                    card.findViewById(R.id.address_select_full);
            RadioButton radioBtn =
                    card.findViewById(R.id.address_select_radio);
            com.google.android.material.card.MaterialCardView cardView =
                    (com.google.android.material.card.MaterialCardView) card;

            tvNickname.setText(addr.getAddressName());
            tvAddress.setText(addr.getAddress());

            if (addr.isDefault() && selectedAddress == null) {
                selectedAddress = addr;
                radioBtn.setChecked(true);
                binding.shippingDetailsAddressName
                        .setText(addr.getAddressName());
                binding.shippingDetailsAddress.setText(addr.getAddress());
            }

            card.setOnClickListener(v -> {
                selectedAddress = addr;
                for (int j = 0; j < container.getChildCount(); j++) {
                    View child = container.getChildAt(j);
                    RadioButton rb =
                            child.findViewById(R.id.address_select_radio);
                    if (rb != null) rb.setChecked(false);
                }
                radioBtn.setChecked(true);
                binding.shippingDetailsAddressName
                        .setText(addr.getAddressName());
                binding.shippingDetailsAddress.setText(addr.getAddress());
            });

            container.addView(card);
        }
    }

    private void getCartItems(FirestoreCallback<List<CartItem>> callback) {
        String uid = firebaseAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(qds -> {
                    if (!qds.isEmpty())
                        callback.onCallback(qds.toObjects(CartItem.class));
                });
    }

    private void getProductsByIds(List<String> ids,
                                  FirestoreCallback<Map<String, Product>> cb) {
        Map<String, Product> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            cb.onCallback(map);
            return;
        }
        db.collection("products").whereIn("productId", ids).get()
                .addOnSuccessListener(qds -> {
                    qds.getDocuments().forEach(ds -> {
                        Product p = ds.toObject(Product.class);
                        if (p != null) map.put(p.getProductId(), p);
                    });
                    cb.onCallback(map);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}