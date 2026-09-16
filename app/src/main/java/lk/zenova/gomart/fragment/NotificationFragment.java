package lk.zenova.gomart.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import lk.zenova.gomart.R;
import lk.zenova.gomart.adapter.NotificationAdapter;
import lk.zenova.gomart.databinding.FragmentNotificationBinding;
import lk.zenova.gomart.helper.NotificationRepository;
import lk.zenova.gomart.model.Notification;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private NotificationRepository repository;
    private NotificationAdapter adapter;
    private final List<Notification> notifList = new ArrayList<>();
    private ListenerRegistration notifListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(
                inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        repository  = new NotificationRepository();

        if (currentUser == null) {
            binding.notifEmptyState.setVisibility(View.VISIBLE);
            binding.notifRecycler.setVisibility(View.GONE);
            return;
        }

        setupRecyclerView();
        listenForNotifications();
        setupMarkAllRead();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notifList,
                notification -> {
                    if (!notification.isRead()) {
                        repository.markAsRead(
                                currentUser.getUid(),
                                notification.getNotifId());
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                        updateUnreadBanner();
                        updateMarkAllReadButton();
                    }

                    if ("ORDER".equals(notification.getType())
                            && notification.getOrderId() != null
                            && !notification.getOrderId().isEmpty()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("orderId",
                                notification.getOrderId());
                        OrderDetailFragment f =
                                new OrderDetailFragment();
                        f.setArguments(bundle);
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, f)
                                .addToBackStack(null)
                                .commit();
                    }
                });

        adapter.setClearListener((notification, position) -> {
            if (position < 0
                    || position >= notifList.size()) return;

            Notification removed = notifList.get(position);
            notifList.remove(position);
            adapter.notifyItemRemoved(position);
            updateEmptyState();
            updateUnreadBanner();
            updateMarkAllReadButton();

            db.collection("notifications")
                    .document(currentUser.getUid())
                    .collection("items")
                    .document(removed.getNotifId())
                    .delete()
                    .addOnFailureListener(e -> {
                        notifList.add(position, removed);
                        adapter.notifyItemInserted(position);
                        Toast.makeText(getContext(),
                                "Failed to delete",
                                Toast.LENGTH_SHORT).show();
                    });

            Snackbar.make(binding.getRoot(),
                            "Notification removed",
                            Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        db.collection("notifications")
                                .document(currentUser.getUid())
                                .collection("items")
                                .document(removed.getNotifId())
                                .set(removed);
                    })
                    .setActionTextColor(
                            android.graphics.Color
                                    .parseColor("#FF6600"))
                    .show();
        });

        binding.notifRecycler.setLayoutManager(
                new LinearLayoutManager(getContext()));
        binding.notifRecycler.setAdapter(adapter);
        binding.notifRecycler.addItemDecoration(
                new DividerItemDecoration(requireContext(),
                        DividerItemDecoration.VERTICAL));
    }


    private void listenForNotifications() {
        notifListener = db
                .collection("notifications")
                .document(currentUser.getUid())
                .collection("items")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null
                            || binding == null) return;

                    notifList.clear();
                    for (var doc : snapshots.getDocuments()) {
                        Notification notif =
                                doc.toObject(Notification.class);
                        if (notif != null) {
                            notif.setNotifId(doc.getId());
                            notifList.add(notif);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    updateUnreadBanner();
                    updateMarkAllReadButton();
                });
    }

    private void setupMarkAllRead() {
        binding.notifMarkAllRead.setOnClickListener(v -> {
            for (Notification n : notifList) {
                if (!n.isRead()) {
                    n.setRead(true);
                }
            }
            repository.markAllAsRead(currentUser.getUid());

            adapter.notifyDataSetChanged();
            updateUnreadBanner();
            updateMarkAllReadButton();

            Toast.makeText(getContext(),
                    "All marked as read",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateMarkAllReadButton() {
        if (binding == null) return;
        long unread = notifList.stream()
                .filter(n -> !n.isRead()).count();
        if (unread > 0) {
            binding.notifMarkAllRead
                    .setBackgroundTintList(
                            android.content.res.ColorStateList
                                    .valueOf(android.graphics.Color
                                            .parseColor("#FF6600")));
        } else {
            binding.notifMarkAllRead
                    .setBackgroundTintList(
                            android.content.res.ColorStateList
                                    .valueOf(android.graphics.Color
                                            .parseColor("#22C55E")));
        }
    }

    private void updateEmptyState() {
        if (binding == null) return;
        if (notifList.isEmpty()) {
            binding.notifEmptyState.setVisibility(View.VISIBLE);
            binding.notifRecycler.setVisibility(View.GONE);
            binding.notifUnreadBanner.setVisibility(View.GONE);
        } else {
            binding.notifEmptyState.setVisibility(View.GONE);
            binding.notifRecycler.setVisibility(View.VISIBLE);
        }
    }

    private void updateUnreadBanner() {
        if (binding == null) return;
        long unread = notifList.stream()
                .filter(n -> !n.isRead()).count();
        if (unread > 0) {
            binding.notifUnreadBanner.setVisibility(View.VISIBLE);
            binding.notifUnreadCount.setText(
                    unread + " unread notification"
                            + (unread > 1 ? "s" : ""));
        } else {
            binding.notifUnreadBanner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notifListener != null) notifListener.remove();
        binding = null;
    }
}