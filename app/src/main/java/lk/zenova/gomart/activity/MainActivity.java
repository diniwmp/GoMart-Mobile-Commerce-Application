package lk.zenova.gomart.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import lk.zenova.gomart.R;
import lk.zenova.gomart.databinding.ActivityMainBinding;
import lk.zenova.gomart.databinding.SideNavHeaderBinding;
import lk.zenova.gomart.fragment.CartFragment;
import lk.zenova.gomart.fragment.CategoryFragment;
import lk.zenova.gomart.fragment.HomeFragment;
import lk.zenova.gomart.fragment.MessageFragment;
import lk.zenova.gomart.fragment.NoInternetFragment;
import lk.zenova.gomart.fragment.NotificationFragment;
import lk.zenova.gomart.fragment.OrdersFragment;
import lk.zenova.gomart.fragment.ProfileFragment;
import lk.zenova.gomart.fragment.SettingsFragment;
import lk.zenova.gomart.fragment.WishlistFragment;
import lk.zenova.gomart.helper.AppPreferences;
import lk.zenova.gomart.helper.BrightnessAdvisor;
import lk.zenova.gomart.helper.LightSensorManager;
import lk.zenova.gomart.model.Address;
import lk.zenova.gomart.model.User;
import lk.zenova.gomart.receiver.ConnectivityReceiver;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;
    private AppPreferences appPreferences;

    private TextView headerAddressNickname;
    private LinearLayout headerAddressLayout;
    private android.widget.FrameLayout headerNotifBtn;
    private View headerSpacer;
    private TextView headerNotifBadge;

    private LightSensorManager lightSensorManager;
    private BrightnessAdvisor brightnessAdvisor;

    private Boolean lastAutoThemeDark = null;

    private ConnectivityReceiver connectivityReceiver;
    private com.google.firebase.firestore.ListenerRegistration userListener;

    private com.google.firebase.firestore.ListenerRegistration addressListener;

    private final BroadcastReceiver connectivityListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isOnline = intent.getBooleanExtra(
                    ConnectivityReceiver.EXTRA_IS_ONLINE, true);
            updateOfflineBanner(isOnline);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appPreferences = new AppPreferences(this);
        if (appPreferences.isDarkTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.BLACK);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth      = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage   = FirebaseStorage.getInstance();

        View headerView = binding.sideNavigationView.getHeaderView(0);
        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        drawerLayout         = binding.drawerLayout;
        navigationView       = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            drawerLayout.closeDrawer(GravityCompat.END);
                            return;
                        }

                        Fragment current = getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_container);

                        if (current instanceof HomeFragment) {
                            showExitDialog();
                        } else {
                            loadFragment(new HomeFragment());
                            navigationView.getMenu()
                                    .findItem(R.id.side_nav_home).setChecked(true);
                            bottomNavigationView.getMenu()
                                    .findItem(R.id.bottom_nav_home).setChecked(true);
                        }
                    }
                });


        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu()
                    .findItem(R.id.bottom_nav_home).setChecked(true);
        }

        loadUserInfo();
        setupHeader();
        requestNotificationPermission();
        setupLightSensor();
        setupNavColors();
        saveFcmToken();
        updateOfflineBanner(ConnectivityReceiver.isOnline(this));
    }


    private void showExitDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setIcon(R.drawable.splash_app_icon)
                .setTitle("Exit GoMart?")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Save FCM token
    private void saveFcmToken() {
        if (firebaseAuth.getCurrentUser() == null) return;
        com.google.firebase.messaging.FirebaseMessaging
                .getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null) return;
                    String uid = firebaseAuth
                            .getCurrentUser().getUid();
                    firebaseFirestore
                            .collection("users")
                            .document(uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener(unused ->
                                    Log.d("FCM",
                                            "Token saved: " + token));
                });
    }

    private void setupNavColors() {
        ColorStateList navColors = ContextCompat.getColorStateList(
                this, R.color.nav_item_color);

        bottomNavigationView.setItemIconTintList(navColors);
        bottomNavigationView.setItemTextColor(navColors);

        navigationView.setItemIconTintList(navColors);
        navigationView.setItemTextColor(navColors);
    }

    private void setupHeader() {
        headerAddressLayout   = binding.headerAddressLayout;
        headerAddressNickname = binding.headerAddressNickname;
        headerNotifBtn        = binding.headerNotifBtn;
        headerSpacer          = binding.headerSpacer;
        headerNotifBadge      = binding.headerNotifBadge;

        headerNotifBtn.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, SignInActivity.class));
                return;
            }
            loadFragment(new NotificationFragment());
        });

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            loadDefaultAddress(user.getUid());
            listenForUnreadNotifications(user.getUid());
        } else {
            headerAddressLayout.setVisibility(View.GONE);
            headerSpacer.setVisibility(View.VISIBLE);
            updateSpacerText("Sign in to GoMart",
                    R.drawable.login_24);
            headerSpacer.setOnClickListener(v ->
                    startActivity(new Intent(this,
                            SignInActivity.class)));
        }
    }

    private void loadDefaultAddress(String userId) {
        if (addressListener != null) {
            addressListener.remove();
            addressListener = null;
        }

        addressListener = firebaseFirestore.collection("addresses")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDefault", true)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || binding == null) return;

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Address address = snapshots.getDocuments()
                                .get(0).toObject(Address.class);

                        if (address != null
                                && address.getAddressName() != null
                                && !address.getAddressName().isEmpty()) {

                            headerAddressNickname.setText(
                                    address.getAddressName());
                            headerAddressLayout.setVisibility(View.VISIBLE);
                            headerSpacer.setVisibility(View.GONE);

                            headerAddressLayout.setOnClickListener(v ->
                                    startActivity(new Intent(this,
                                            AddAddressActivity.class)));
                        } else {
                            showSetDeliveryLocation();
                        }
                    } else {
                        showSetDeliveryLocation();
                    }
                });
    }
    private void showSetDeliveryLocation() {
        headerAddressLayout.setVisibility(View.GONE);
        headerSpacer.setVisibility(View.VISIBLE);

        updateSpacerText("Set delivery location",
                R.drawable.location_24);

        headerSpacer.setOnClickListener(v ->
                startActivity(new Intent(this,
                        AddAddressActivity.class)));
    }

    private void updateSpacerText(String text, int iconRes) {
        if (binding == null) return;
        LinearLayout spacer = binding.headerSpacer;
        if (spacer.getChildCount() >= 2) {
            android.widget.ImageView icon =
                    (android.widget.ImageView) spacer.getChildAt(0);
            TextView label =
                    (TextView) spacer.getChildAt(1);
            icon.setImageResource(iconRes);
            label.setText(text);
        }
    }

    private void listenForUnreadNotifications(String userId) {
        firebaseFirestore
                .collection("notifications")
                .document(userId)
                .collection("items")
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    int count = snapshots.size();
                    runOnUiThread(() -> {
                        if (count > 0) {
                            headerNotifBadge.setVisibility(View.VISIBLE);
                            headerNotifBadge.setText(
                                    count > 9 ? "9+" : String.valueOf(count));
                        } else {
                            headerNotifBadge.setVisibility(View.GONE);
                        }
                    });
                });
    }


    private void setupLightSensor() {
        lightSensorManager = new LightSensorManager(this);
        if (!lightSensorManager.isAvailable()) return;

        brightnessAdvisor = new BrightnessAdvisor(this, binding.getRoot());

        lightSensorManager.setOnLuxReadingListener(lux -> {
            if (appPreferences.isAutoThemeEnabled()) {
                boolean isDark = LightSensorManager.isDarkEnvironment(lux);

                if (lastAutoThemeDark == null || lastAutoThemeDark != isDark) {
                    lastAutoThemeDark = isDark;

                    int targetMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

                    runOnUiThread(() -> {
                        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                            appPreferences.setTheme(isDark ? AppPreferences.THEME_DARK : AppPreferences.THEME_LIGHT);
                            AppCompatDelegate.setDefaultNightMode(targetMode);
                        }
                    });
                }
            }

            if (appPreferences.isAutoBrightnessEnabled()) {
                runOnUiThread(() -> brightnessAdvisor.evaluate(lux));
            }
        });
    }

    private void updateOfflineBanner(boolean isOnline) {
        if (isOnline) {
            NoInternetFragment existing =
                    (NoInternetFragment) getSupportFragmentManager()
                            .findFragmentByTag("no_internet");
            if (existing != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(existing)
                        .commitAllowingStateLoss();
            }
        } else {
            if (getSupportFragmentManager()
                    .findFragmentByTag("no_internet") == null) {

                NoInternetFragment noInternet = new NoInternetFragment();

                noInternet.setOnRetryListener(() -> {
                    if (ConnectivityReceiver.isOnline(this)) {
                        updateOfflineBanner(true);
                    }
                });

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,
                                noInternet, "no_internet")
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (connectivityReceiver == null) {
            connectivityReceiver = new ConnectivityReceiver();
        }
        registerReceiver(connectivityReceiver,
                new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                connectivityListener,
                new IntentFilter(ConnectivityReceiver.ACTION_CONNECTIVITY_CHANGED));

        updateOfflineBanner(ConnectivityReceiver.isOnline(this));

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            loadUserInfo();
        }

        if (appPreferences != null && !appPreferences.isAutoThemeEnabled()) {
            int targetMode = appPreferences.isDarkTheme()
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;

            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
            }
        }

        lastAutoThemeDark = null;
        if (lightSensorManager != null) lightSensorManager.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        try { unregisterReceiver(connectivityReceiver); }
        catch (IllegalArgumentException ignored) {}
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(connectivityListener);


        if (lightSensorManager != null) lightSensorManager.stop();
        if (brightnessAdvisor != null) {
            brightnessAdvisor.restoreSystemBrightness();
        }
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            if (userListener != null) {
                userListener.remove();
                userListener = null;
            }
            return;
        }

        if (userListener != null) return;

        userListener = firebaseFirestore.collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((ds, e) -> {
                    if (e != null) {
                        Log.e("loadUserInfo", "Listen failed.", e);
                        return;
                    }

                    if (binding == null || sideNavHeaderBinding == null) return;
                    if (ds == null || !ds.exists()) return;

                    User firestoreUser = ds.toObject(User.class);
                    if (firestoreUser == null) return;

                    sideNavHeaderBinding.headerUserName.setText(
                            firestoreUser.getName() != null
                                    ? firestoreUser.getName() : "");
                    sideNavHeaderBinding.headerUserEmail.setText(
                            firestoreUser.getEmail() != null
                                    ? firestoreUser.getEmail() : "");

                    String picUrl = firestoreUser.getProfilePicUrl();
                    Log.d("ProfilePic", "URL: [" + picUrl + "]");

                    if (picUrl != null && !picUrl.isEmpty()) {
                        if (picUrl.startsWith("https://")) {
                            if (!isDestroyed() && !isFinishing()) {
                                Glide.with(MainActivity.this)
                                        .load(picUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.person_24)
                                        .error(R.drawable.person_24)
                                        .into(sideNavHeaderBinding.headerProfilePic);
                            }
                        } else {
                            firebaseStorage.getReference("profile-images")
                                    .child(picUrl)
                                    .getDownloadUrl()
                                    .addOnSuccessListener(uri -> {

                                        if (!isDestroyed() && !isFinishing()
                                                && binding != null
                                                && sideNavHeaderBinding != null) {
                                            Glide.with(MainActivity.this)
                                                    .load(uri)
                                                    .circleCrop()
                                                    .placeholder(R.drawable.person_24)
                                                    .error(R.drawable.person_24)
                                                    .into(sideNavHeaderBinding.headerProfilePic);
                                        }
                                    })
                                    .addOnFailureListener(err ->
                                            Log.e("loadUserInfo", "Image URL failed: " + err.getMessage()));
                        }
                    } else {
                        sideNavHeaderBinding.headerProfilePic
                                .setImageResource(R.drawable.person_24);
                    }

                    navigationView.getMenu()
                            .findItem(R.id.side_nav_login).setVisible(false);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_profile).setVisible(true);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_orders).setVisible(true);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_wishlist).setVisible(true);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_cart).setVisible(true);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_message).setVisible(true);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_logout).setVisible(true);
                    navigationView.getMenu()
                            .findItem(R.id.side_nav_category).setVisible(true);
                });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.bottom_nav_menu) {
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        Menu navMenu    = navigationView.getMenu();
        Menu bottomMenu = bottomNavigationView.getMenu();
        for (int i = 0; i < navMenu.size(); i++)
            navMenu.getItem(i).setChecked(false);
        for (int i = 0; i < bottomMenu.size(); i++)
            bottomMenu.getItem(i).setChecked(false);

        if (itemId == R.id.side_nav_home
                || itemId == R.id.bottom_nav_home) {
            loadFragment(new HomeFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu()
                    .findItem(R.id.bottom_nav_home).setChecked(true);

        } else if (itemId == R.id.side_nav_profile
                || itemId == R.id.bottom_nav_profile) {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            }
            loadFragment(new ProfileFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_profile).setChecked(true);
            bottomNavigationView.getMenu()
                    .findItem(R.id.bottom_nav_profile).setChecked(true);

        } else if (itemId == R.id.side_nav_orders) {
            loadFragment(new OrdersFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_orders).setChecked(true);

        } else if (itemId == R.id.side_nav_category) {
            loadFragment(new CategoryFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_category).setChecked(true);

        } else if (itemId == R.id.side_nav_cart
                || itemId == R.id.bottom_nav_cart) {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            }
            loadFragment(new CartFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_cart).setChecked(true);
            bottomNavigationView.getMenu()
                    .findItem(R.id.bottom_nav_cart).setChecked(true);

        } else if (itemId == R.id.side_nav_message) {
            loadFragment(new MessageFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_message).setChecked(true);

        } else if (itemId == R.id.side_nav_settings) {
            loadFragment(new SettingsFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_settings).setChecked(true);

        } else if (itemId == R.id.side_nav_wishlist
                || itemId == R.id.bottom_nav_wishlist) {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            }
            loadFragment(new WishlistFragment());
            navigationView.getMenu()
                    .findItem(R.id.side_nav_wishlist).setChecked(true);
            bottomNavigationView.getMenu()
                    .findItem(R.id.bottom_nav_wishlist).setChecked(true);

        } else if (itemId == R.id.side_nav_login) {
            startActivity(new Intent(this, SignInActivity.class));

        } else if (itemId == R.id.side_nav_logout) {
            appPreferences.clearSession();
            firebaseAuth.signOut();
            
            if (userListener != null) {
                userListener.remove();
                userListener = null;
            }
            
            loadFragment(new HomeFragment());
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.side_nav_menu);
            
            navigationView.removeHeaderView(sideNavHeaderBinding.getRoot());
            View newHeader = navigationView.inflateHeaderView(R.layout.side_nav_header);
            sideNavHeaderBinding = SideNavHeaderBinding.bind(newHeader);
            
            resetHeaderToLoggedOut();
        }
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }

        return true;
    }
    private void resetHeaderToLoggedOut() {
        headerAddressLayout.setVisibility(View.GONE);
        headerSpacer.setVisibility(View.VISIBLE);
        updateSpacerText("Sign in to GoMart", R.drawable.login_24);

        headerSpacer.setOnClickListener(v ->
                startActivity(new Intent(this, SignInActivity.class)));

        headerNotifBadge.setVisibility(View.GONE);
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        if (fragment instanceof HomeFragment) {
            fm.popBackStack(null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (addressListener != null) {
            addressListener.remove();
            addressListener = null;
        }
    }
}