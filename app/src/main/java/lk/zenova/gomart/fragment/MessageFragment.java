package lk.zenova.gomart.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.zenova.gomart.adapter.MessageAdapter;
import lk.zenova.gomart.databinding.FragmentMessageBinding;
import lk.zenova.gomart.model.Message;

public class MessageFragment extends Fragment {


    private FragmentMessageBinding binding;

    private FirebaseFirestore db;
    private FirebaseAuth      auth;
    private FirebaseUser      currentUser;

    private MessageAdapter     adapter;
    private final List<Message> messageList = new ArrayList<>();
    private ListenerRegistration messageListener;

    private String userId   = "";
    private String userName = "User";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db          = FirebaseFirestore.getInstance();
        auth        = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            binding.msgInput.setEnabled(false);
            binding.msgSendBtn.setEnabled(false);
            binding.msgOnlineStatus.setText("Sign in to chat");
            return;
        }

        userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(ds -> {
                    if (ds.exists() && ds.getString("name") != null) {
                        userName = ds.getString("name");
                    }
                });

        setupRecyclerView();
        createChatIfNotExists();
        loadMessages();
        setupSendButton();
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messageList, userId);

        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);

        binding.msgRecycler.setLayoutManager(lm);
        binding.msgRecycler.setAdapter(adapter);
    }

    private void loadMessages() {
        messageListener = db
                .collection("chats")
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) return;

                    messageList.clear();

                    for (var doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getId());
                            messageList.add(msg);
                        }
                    }

                    adapter.notifyDataSetChanged();


                    if (!messageList.isEmpty()) {
                        binding.msgRecycler.scrollToPosition(
                                messageList.size() - 1);
                    }
                });
    }

    private void setupSendButton() {
        binding.msgSendBtn.setOnClickListener(v -> {

            String text = binding.msgInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            binding.msgInput.setText("");

            Message message = new Message(
                    text,
                    userId,
                    userName,
                    false
            );

            db.collection("chats")
                    .document(userId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(ref -> updateChatMetadata(text))
                    .addOnFailureListener(e -> {
                        binding.msgInput.setText(text);
                    });
        });
    }


    private void createChatIfNotExists() {
        DocumentReference chatRef = db.collection("chats").document(userId);

        chatRef.get().addOnSuccessListener(ds -> {
            if (!ds.exists()) {
                Map<String, Object> chatMeta = new HashMap<>();
                chatMeta.put("userId",        userId);
                chatMeta.put("userName",      userName);
                chatMeta.put("userEmail",     currentUser.getEmail() != null
                        ? currentUser.getEmail() : "");
                chatMeta.put("lastMessage",   "Chat started");
                chatMeta.put("lastTimestamp", Timestamp.now());
                chatMeta.put("unreadCount",   0);

                chatRef.set(chatMeta)
                        .addOnSuccessListener(unused -> sendWelcomeMessage());
            }
        });
    }

    private void sendWelcomeMessage() {
        Message welcome = new Message(
                "Hello! Welcome to GoMart Support.\n"
                        + "How can we help you today?",
                "ADMIN",
                "GoMart Support",
                true
        );

        db.collection("chats")
                .document(userId)
                .collection("messages")
                .add(welcome);
    }


    private void updateChatMetadata(String lastMessage) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastMessage",   lastMessage);
        update.put("lastTimestamp", Timestamp.now());
        update.put("unreadCount",   FieldValue.increment(1));
        update.put("userName",      userName);
        update.put("userEmail",     currentUser.getEmail() != null
                ? currentUser.getEmail() : "");

        db.collection("chats")
                .document(userId)
                .update(update);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messageListener != null) messageListener.remove();
        binding = null;
    }
}
