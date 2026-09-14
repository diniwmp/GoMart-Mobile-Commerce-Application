package lk.zenova.gomart.activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import lk.zenova.gomart.R;
import lk.zenova.gomart.databinding.ActivitySignInBinding;
import lk.zenova.gomart.helper.AppPreferences;
import lk.zenova.gomart.helper.NotificationHelper;
import lk.zenova.gomart.model.User;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private AppPreferences appPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySignInBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        appPreferences = new AppPreferences(this);

        if (firebaseAuth.getCurrentUser() != null) {
            saveFcmToken();
        }


        if (appPreferences.isRememberMe()) {
            binding.signinInputEmail.setText(
                    appPreferences.getSavedEmail());
            binding.signinInputPassword.setText(
                    appPreferences.getSavedPassword());
            binding.signinCheckRememberMe
                    .setChecked(true);
        }

        if (appPreferences.isLoggedIn()
                && firebaseAuth.getCurrentUser()
                != null) {
            goToMain();
            return;
        }

        binding.signintextSignup.setOnClickListener(
                view -> {
                    startActivity(new Intent(
                            SignInActivity.this,
                            SignUpActivity.class));
                    finish();
                });

        binding.signinForgotPassword
                .setOnClickListener(v ->
                        showForgotPasswordDialog());

        binding.signinBtnSignin.setOnClickListener(
                view -> handleSignIn());
    }


    private void showForgotPasswordDialog() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE);
        dialog.setContentView(
                R.layout.dialog_forgot_password);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources()
                            .getDisplayMetrics().widthPixels
                            * 0.9f),
                    android.view.ViewGroup.LayoutParams
                            .WRAP_CONTENT);
        }

        EditText emailInput = dialog.findViewById(
                R.id.forgot_email_input);
        Button sendBtn = dialog.findViewById(
                R.id.forgot_send_btn);
        TextView cancelBtn = dialog.findViewById(
                R.id.forgot_cancel_btn);
        ProgressBar progressBar = dialog.findViewById(
                R.id.forgot_progress);
        TextView successMsg = dialog.findViewById(
                R.id.forgot_success_msg);


        String existingEmail = binding
                .signinInputEmail
                .getText().toString().trim();
        if (!existingEmail.isEmpty()) {
            emailInput.setText(existingEmail);
        }

        cancelBtn.setOnClickListener(v ->
                dialog.dismiss());

        sendBtn.setOnClickListener(v -> {

            String email = emailInput.getText()
                    .toString().trim();

            if (email.isEmpty()) {
                emailInput.setError(
                        "Please enter your email");
                emailInput.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS
                    .matcher(email).matches()) {
                emailInput.setError(
                        "Enter a valid email address");
                emailInput.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            sendBtn.setEnabled(false);
            sendBtn.setText("Sending...");
            successMsg.setVisibility(View.GONE);

            Log.d("ForgotPassword",
                    "Sending reset to: " + email);

            firebaseAuth
                    .sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> {

                        Log.d("ForgotPassword",
                                "Reset email sent "
                                        + "successfully to: "
                                        + email);

                        progressBar.setVisibility(
                                View.GONE);
                        sendBtn.setVisibility(View.GONE);

                        successMsg.setText(
                                "Reset link sent to:\n"
                                        + email
                                        + "\n\nPlease check your inbox"
                                        + "\nEmail may take "
                                        + "1-2 minutes to arrive.");
                        successMsg.setVisibility(View.VISIBLE);

                        binding.signinInputEmail
                                .setText(email);
                        binding.signinInputPassword
                                .setText("");

                        binding.getRoot().postDelayed(
                                dialog::dismiss, 4000);
                    })
                    .addOnFailureListener(e -> {

                        Log.e("ForgotPassword",
                                "Reset email FAILED: "
                                        + e.getMessage());
                        Log.e("ForgotPassword",
                                "Error class: "
                                        + e.getClass().getName());

                        progressBar.setVisibility(View.GONE);
                        sendBtn.setEnabled(true);
                        sendBtn.setText("Send Reset Link");

                        String errorMsg;
                        String err = e.getMessage() != null
                                ? e.getMessage().toLowerCase()
                                : "";

                        Log.e("ForgotPassword",
                                "Raw error: " + err);

                        if (err.contains("no user record")
                                || err.contains("user-not-found")
                                || err.contains("there is no user")) {
                            errorMsg =
                                    "No account found with "
                                            + "this email address.\n"
                                            + "Please check the email "
                                            + "or create a new account.";
                        } else if (err.contains(
                                "invalid-email")
                                || err.contains("badly formatted")) {
                            errorMsg =
                                    "Invalid email address format.";
                        } else if (err.contains("network")
                                || err.contains("unable to resolve")
                                || err.contains("timeout")) {
                            errorMsg =
                                    "No internet connection. "
                                            + "Please try again.";
                        } else if (err.contains(
                                "too-many-requests")
                                || err.contains("too many")) {
                            errorMsg =
                                    "Too many attempts. "
                                            + "Please wait a few minutes "
                                            + "and try again.";
                        } else {
                            errorMsg =
                                    "Error: " + e.getMessage();
                        }

                        emailInput.setError(errorMsg);
                        emailInput.requestFocus();

                        Toast.makeText(
                                SignInActivity.this,
                                errorMsg,
                                Toast.LENGTH_LONG).show();
                    });
        });



        dialog.show();
    }


    private void handleSignIn() {
        String email = binding.signinInputEmail
                .getText().toString().trim();
        String password = binding.signinInputPassword
                .getText().toString().trim();
        boolean rememberMe =
                binding.signinCheckRememberMe
                        .isChecked();

        if (email.isEmpty()) {
            binding.signinInputEmail.setError(
                    "Email is required");
            binding.signinInputEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            binding.signinInputEmail.setError(
                    "Enter a valid email");
            binding.signinInputEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            binding.signinInputPassword.setError(
                    "Password is required");
            binding.signinInputPassword.requestFocus();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(
                        email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user =
                                firebaseAuth
                                        .getCurrentUser();
                        if (user != null) {
                            appPreferences.setRememberMe(
                                    rememberMe,
                                    email, password);
                            saveFcmToken(user.getUid());
                            loadUserAndNotify(
                                    user.getUid());
                        }
                    } else {
                        Toast.makeText(
                                        SignInActivity.this,
                                        "Authentication failed."
                                                + " Check your email"
                                                + " or password.",
                                        Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }


    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String uid = FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getUid();
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener(
                                    unused -> Log.d(
                                            "FCM",
                                            "Token saved: "
                                                    + token));
                });
    }

    private void saveFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token ->
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("fcmToken",
                                        token));
    }


    private void loadUserAndNotify(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(ds -> {
                    String name = "there";
                    if (ds.exists()) {
                        User user =
                                ds.toObject(User.class);
                        if (user != null
                                && user.getName() != null
                                && !user.getName()
                                .isEmpty()) {
                            name = user.getName();
                            appPreferences.setLoggedIn(
                                    true,
                                    user.getName(),
                                    user.getEmail());
                        }
                    }
                    showWelcomeNotification(name);
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    showWelcomeNotification("there");
                    goToMain();
                });
    }

    private void showWelcomeNotification(
            String userName) {
        NotificationHelper.showNotification(
                this,
                NotificationHelper.NOTIF_WELCOME,
                NotificationHelper.CHANNEL_GENERAL,
                "Welcome back, " + userName + "!",
                "Great to see you again. "
                        + "Check out today's fresh deals!"
        );
    }

    private void goToMain() {
        startActivity(new Intent(
                SignInActivity.this,
                MainActivity.class));
        finish();
    }
}