package lk.zenova.gomart.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.UUID;

import lk.zenova.gomart.R;
import lk.zenova.gomart.activity.AddAddressActivity;
import lk.zenova.gomart.databinding.FragmentProfileBinding;
import lk.zenova.gomart.model.Address;
import lk.zenova.gomart.model.User;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        loadUserInfo();
        loadAddresses();

        binding.profileImage.setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));


        binding.profileBtnSave.setOnClickListener(v -> saveUserInfo());


        binding.profileBtnAddAddress.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AddAddressActivity.class)));
    }

    private void loadUserInfo() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(ds -> {
                    if (!isAdded() || getActivity() == null
                            || binding == null) return;

                    if (!ds.exists()) return;

                    if (ds.exists()) {
                        User user = ds.toObject(User.class);
                        if (user != null) {
                            binding.profileFullName.setText(user.getName());
                            binding.profileEmail.setText(user.getEmail());
                            binding.profilePhone.setText(user.getMobile());

                            String picUrl = user.getProfilePicUrl();
                            if (picUrl != null && !picUrl.isEmpty()) {
                                if (picUrl.startsWith("https://")) {

                                    if (isAdded() && getActivity() != null) {
                                        Glide.with(this)
                                                .load(picUrl)
                                                .circleCrop()
                                                .placeholder(R.drawable.person)
                                                .into(binding.profileImage);
                                    }
                                } else {
                                    storage.getReference("profile-images")
                                            .child(picUrl)
                                            .getDownloadUrl()
                                            .addOnSuccessListener(uri ->

                                                    Glide.with(this)
                                                            .load(uri)
                                                            .circleCrop()
                                                            .placeholder(R.drawable.person)
                                                            .into(binding.profileImage))
                                            .addOnFailureListener(e ->
                                                    Log.e("Profile",
                                                            "Image URL failed: "
                                                                    + e.getMessage()));
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Profile", "Load user failed: " + e.getMessage()));
    }

    private void saveUserInfo() {
        if (binding == null) return;

        String name  = binding.profileFullName.getText().toString().trim();
        String email = binding.profileEmail.getText().toString().trim();
        String phone = binding.profilePhone.getText().toString().trim();

        if (name.isEmpty()) {
            binding.profileFullName.setError("Full name is required");
            binding.profileFullName.requestFocus();
            return;
        }
        if (name.length() < 3) {
            binding.profileFullName.setError("Name must be at least 3 characters");
            binding.profileFullName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            binding.profileEmail.setError("Email is required");
            binding.profileEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.profileEmail.setError("Enter a valid email address");
            binding.profileEmail.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            binding.profilePhone.setError("Phone number is required");
            binding.profilePhone.requestFocus();
            return;
        }
        if (phone.length() < 9) {
            binding.profilePhone.setError("Enter a valid phone number");
            binding.profilePhone.requestFocus();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        binding.profileBtnSave.setEnabled(false);
        binding.profileBtnSave.setText("Saving...");

        db.collection("users").document(uid)
                .update("name", name, "email", email, "mobile", phone)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null || !isAdded()) return;
                    binding.profileBtnSave.setEnabled(true);
                    binding.profileBtnSave.setText("Save Changes");
                    Toast.makeText(getContext(),
                            "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isAdded()) return;
                    binding.profileBtnSave.setEnabled(true);
                    binding.profileBtnSave.setText("Save Changes");
                    Toast.makeText(getContext(),
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAddresses() {
        if (firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        db.collection("addresses")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(qds -> {
                    if (binding == null || !isAdded()) return;

                    binding.profileAddressList.removeAllViews();

                    if (qds.isEmpty()) {
                        binding.profileNoAddress.setVisibility(View.VISIBLE);
                        return;
                    }

                    binding.profileNoAddress.setVisibility(View.GONE);

                    List<Address> addresses = qds.toObjects(Address.class);

                    for (int i = 0; i < addresses.size(); i++) {
                        Address addr = addresses.get(i);
                        String docId = qds.getDocuments().get(i).getId();

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_address_card,
                                        binding.profileAddressList, false);

                        TextView tvNickname = card.findViewById(
                                R.id.address_card_nickname);
                        TextView tvFull     = card.findViewById(
                                R.id.address_card_full);
                        TextView tvBadge    = card.findViewById(
                                R.id.address_card_default_badge);
                        CheckBox checkBox   = card.findViewById(
                                R.id.address_card_checkbox);

                        tvNickname.setText(addr.getAddressName());
                        tvFull.setText(addr.getAddress());
                        checkBox.setChecked(addr.isDefault());
                        tvBadge.setVisibility(
                                addr.isDefault() ? View.VISIBLE : View.GONE);

                        card.setOnClickListener(v ->
                                setDefaultAddress(docId, uid, addr));

                        ImageView btnDelete = card.findViewById(R.id.address_card_btn_delete);
                        btnDelete.setOnClickListener(v -> deleteAddress(docId, addr.getAddressName()));

                        binding.profileAddressList.addView(card);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Profile", "Load addresses failed: " + e.getMessage()));
    }

    private void deleteAddress(String docId, String addressName) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Address")
                .setMessage("Remove \"" + addressName + "\" from your saved addresses?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("addresses").document(docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(),
                                        "\"" + addressName + "\" removed.",
                                        Toast.LENGTH_SHORT).show();
                                loadAddresses();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed to delete: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void setDefaultAddress(String selectedDocId, String uid,
                                   Address selectedAddr) {
        db.collection("addresses")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(qds -> {
                    for (int i = 0; i < qds.getDocuments().size(); i++) {
                        String docId = qds.getDocuments().get(i).getId();
                        db.collection("addresses").document(docId)
                                .update("isDefault", docId.equals(selectedDocId));
                    }
                    Toast.makeText(getContext(),
                            selectedAddr.getAddressName() + " set as default",
                            Toast.LENGTH_SHORT).show();
                    loadAddresses();
                });
    }

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(), uri -> {
                        if (uri != null) uploadProfileImage(uri);
                    });

    private void uploadProfileImage(Uri uri) {
        if (firebaseAuth.getCurrentUser() == null || binding == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();

        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.profileImage);

        String imageId = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference("profile-images")
                .child(imageId);

        ref.putFile(uri)
                .addOnSuccessListener(task ->
                        db.collection("users").document(uid)
                                .update("profilePicUrl", imageId)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(getContext(),
                                                "Profile photo updated!",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to save photo URL",
                                                Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAddresses();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}