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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.adapter.OrdersAdapter;
import lk.zenova.gomart.databinding.FragmentOrdersBinding;
import lk.zenova.gomart.model.Order;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadOrders();
    }

    private void loadOrders() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to view orders", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("orders")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null) return;

                    if (qds.isEmpty()) {
                        binding.ordersEmptyState.setVisibility(View.VISIBLE);
                        binding.ordersRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    List<Order> orders = qds.toObjects(Order.class);

                    orders.sort((o1, o2) -> {
                        if (o1.getOrderDate() == null || o2.getOrderDate() == null) return 0;
                        return o2.getOrderDate().compareTo(o1.getOrderDate());
                    });

                    binding.ordersRecyclerView.setLayoutManager(
                            new LinearLayoutManager(getContext()));

                    OrdersAdapter adapter = new OrdersAdapter(orders, order -> {
                        OrderDetailFragment detailFragment = new OrderDetailFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("orderId", order.getOrderId());
                        detailFragment.setArguments(bundle);

                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, detailFragment)
                                .addToBackStack(null)
                                .commit();
                    });

                    binding.ordersRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}