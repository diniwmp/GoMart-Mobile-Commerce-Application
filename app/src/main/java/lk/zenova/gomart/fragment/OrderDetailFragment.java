package lk.zenova.gomart.fragment;

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

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import lk.zenova.gomart.adapter.OrderDetailProductAdapter;
import lk.zenova.gomart.databinding.FragmentOrderDetailBinding;
import lk.zenova.gomart.model.Order;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private String orderId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString("orderId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.orderDetailBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }
                        });

        loadOrderDetail();
    }

    private void loadOrderDetail() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("orders")
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null || !isAdded()) return;

                    if (qds.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Order not found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Order order = qds.getDocuments()
                            .get(0).toObject(Order.class);
                    if (order == null) return;

                    String shortId = order.getOrderId();
                    if (shortId != null
                            && shortId.length() > 8) {
                        shortId = "#" + shortId.substring(
                                shortId.length() - 8);
                    }
                    binding.orderDetailId.setText(shortId);

                    if (order.getOrderDate() != null) {
                        SimpleDateFormat sdf =
                                new SimpleDateFormat(
                                        "dd MMM yyyy, hh:mm a",
                                        Locale.getDefault());
                        binding.orderDetailDate.setText(
                                sdf.format(order.getOrderDate()
                                        .toDate()));
                    }

                    String status = order.getStatus();
                    String statusUpper = status != null
                            ? status.toUpperCase() : "PROCESSING";
                    binding.orderDetailStatus.setText(statusUpper);
                    setStatusColor(statusUpper);

                    binding.orderDetailTotal.setText(
                            String.format(Locale.US,
                                    "LKR %,.2f",
                                    order.getTotalAmount()));


                    boolean hasBillingAddress =
                            order.getBillingAddress() != null
                                    && order.getBillingAddress()
                                    .getAddress() != null
                                    && !order.getBillingAddress()
                                    .getAddress().trim().isEmpty();

                    if (hasBillingAddress) {

                        Order.Address billing =
                                order.getBillingAddress();

                        binding.orderDetailShippingName.setText(
                                billing.getName() != null
                                        ? billing.getName() : "—");
                        binding.orderDetailShippingContact.setText(
                                billing.getContact() != null
                                        ? billing.getContact() : "—");
                        binding.orderDetailShippingAddress.setText(
                                billing.getAddress() != null
                                        ? billing.getAddress() : "—");

                    } else {
                        if (order.getShippingAddress() != null) {
                            Order.Address addr =
                                    order.getShippingAddress();
                            binding.orderDetailShippingName.setText(
                                    addr.getName() != null
                                            ? addr.getName() : "—");
                            binding.orderDetailShippingContact.setText(
                                    addr.getContact() != null
                                            ? addr.getContact() : "—");
                            binding.orderDetailShippingAddress.setText(
                                    addr.getAddress() != null
                                            ? addr.getAddress() : "—");
                        }
                    }

                    List<Order.OrderItem> items =
                            order.getOrderItems();
                    if (items != null && !items.isEmpty()) {
                        binding.orderDetailItemsRecycler
                                .setLayoutManager(
                                        new LinearLayoutManager(
                                                getContext()));
                        OrderDetailProductAdapter adapter =
                                new OrderDetailProductAdapter(items);
                        binding.orderDetailItemsRecycler
                                .setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to load order details",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setStatusColor(String status) {
        if (status == null || binding == null) return;
        int color;
        switch (status.toUpperCase()) {
            case "PAID":       color = 0xFF4CAF50; break;
            case "PROCESSING": color = 0xFF1E40AF; break;
            case "SHIPPED":    color = 0xFFFA9C28; break;
            case "DELIVERED":  color = 0xFF009688; break;
            case "CANCELLED":  color = 0xFFF44336; break;
            default:           color = 0xFF9E9E9E; break;
        }
        binding.orderDetailStatus.setTextColor(color);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}