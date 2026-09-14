package lk.zenova.gomart.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import lk.zenova.gomart.R;

public class VisitShopActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;

    private final LatLng shop1 = new LatLng(6.9359633, 79.9291433);
    private final LatLng shop2 = new LatLng(6.9271,    79.8612);
    private final LatLng shop3 = new LatLng(6.9147,    79.9730);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_shop);


        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        Button btnDirections = findViewById(R.id.btn_get_directions);
        btnDirections.setOnClickListener(v -> openGoogleMapsDirections());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.addMarker(new MarkerOptions()
                .position(shop1)
                .title("GoMart - Main Branch")
                .snippet("Tap for directions")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_ORANGE)));

        mMap.addMarker(new MarkerOptions()
                .position(shop2)
                .title("GoMart - Colombo Branch")
                .snippet("Tap for directions")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_ORANGE)));

        mMap.addMarker(new MarkerOptions()
                .position(shop3)
                .title("GoMart - Kotte Branch")
                .snippet("Tap for directions")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_ORANGE)));


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shop1, 12));


        mMap.setOnMarkerClickListener(marker -> {
            openDirectionsToLocation(marker.getPosition());
            return false;
        });
    }

    private void openGoogleMapsDirections() {
        String uri = "geo:6.9359633,79.9291433?q=GoMart+Shop+Sri+Lanka";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=GoMart+Shop+Sri+Lanka"));
            startActivity(browserIntent);
        }
    }

    private void openDirectionsToLocation(LatLng location) {
        String uri = "google.navigation:q="
                + location.latitude + ","
                + location.longitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?daddr="
                            + location.latitude + ","
                            + location.longitude));
            startActivity(browserIntent);
        }
    }
}