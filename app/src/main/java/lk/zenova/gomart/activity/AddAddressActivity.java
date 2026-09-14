package lk.zenova.gomart.activity;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.zenova.gomart.R;

public class AddAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseFirestore db;
    private GoogleMap mMap;

    private TextInputEditText addressName, fullAddress;
    private TextView latText, lngText;
    private CheckBox checkDefaultAddress;
    private MaterialButton addAddressBtn;
    private ImageView resetBtn, btnBack;

    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

        db = FirebaseFirestore.getInstance();

        addressName = findViewById(R.id.addressName);
        fullAddress = findViewById(R.id.fullAddress);
        latText = findViewById(R.id.latText);
        lngText = findViewById(R.id.lngText);
        addAddressBtn = findViewById(R.id.addAddressBtn);
        resetBtn = findViewById(R.id.resetBtn);
        btnBack = findViewById(R.id.btn_back);
        checkDefaultAddress = findViewById(R.id.checkDefaultAddress);

        btnBack.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBVChQRxXh4R4JUQDERVJKfEbvJKE-wOX0");
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
            ));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        latitude = latLng.latitude;
                        longitude = latLng.longitude;

                        if (mMap != null) {
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(place.getName()));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        }

                        latText.setText("Latitude: " + latitude);
                        lngText.setText("Longitude: " + longitude);
                        addressName.setText(place.getName());
                        fullAddress.setText(place.getAddress());
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(AddAddressActivity.this,
                            "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        addAddressBtn.setOnClickListener(v -> saveAddress());
        resetBtn.setOnClickListener(v -> resetFields());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng defaultLocation = new LatLng(6.9271, 79.8612);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));

            latitude = latLng.latitude;
            longitude = latLng.longitude;

            latText.setText("Latitude: " + latitude);
            lngText.setText("Longitude: " + longitude);

            getAddressFromLocation(latitude, longitude);
        });
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                fullAddress.setText(addresses.get(0).getAddressLine(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAddress() {
        String nickname = addressName.getText() != null
                ? addressName.getText().toString().trim() : "";
        String address = fullAddress.getText() != null
                ? fullAddress.getText().toString().trim() : "";

        if (nickname.isEmpty() || address.isEmpty()) {
            Toast.makeText(this,
                    "Please select a location on the map first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this,
                    "Please pin your location on the map",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();
        boolean isDefault = checkDefaultAddress.isChecked();


        if (isDefault) {
            db.collection("addresses")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isDefault", true)
                    .get()
                    .addOnSuccessListener(qds -> {

                        for (com.google.firebase.firestore
                                .DocumentSnapshot doc
                                : qds.getDocuments()) {
                            doc.getReference()
                                    .update("isDefault", false);
                        }

                        saveAddressToFirestore(
                                userId, nickname, address,
                                true);
                    })
                    .addOnFailureListener(e ->

                            saveAddressToFirestore(
                                    userId, nickname, address,
                                    true));
        } else {
            saveAddressToFirestore(
                    userId, nickname, address, false);
        }
    }

    private void saveAddressToFirestore(String userId,
                                        String nickname,
                                        String address,
                                        boolean isDefault) {
        Map<String, Object> data = new HashMap<>();
        data.put("addressName", nickname);
        data.put("address", address);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("userId", userId);
        data.put("isDefault", isDefault);

        db.collection("addresses")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddAddressActivity.this,
                            isDefault
                                    ? "Address saved as default!"
                                    : "Address saved successfully!",
                            Toast.LENGTH_SHORT).show();
                    resetFields();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AddAddressActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    private void resetFields() {
        addressName.setText("");
        fullAddress.setText("");
        checkDefaultAddress.setChecked(false);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        if (autocompleteFragment != null) {
            autocompleteFragment.setText("");
        }

        latText.setText("Latitude");
        lngText.setText("Longitude");
        latitude = 0;
        longitude = 0;

        if (mMap != null) {
            mMap.clear();
        }
    }
}