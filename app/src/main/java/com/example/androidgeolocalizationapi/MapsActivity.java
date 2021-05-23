package com.example.androidgeolocalizationapi;

import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private final int ACCESS_LOCATION_PERMISSION_CODE = 44;
    private FusedLocationProviderClient fusedLocationClient;
    private final int ADD_LOCATION_CODE = 99;
    private EditText address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        address = findViewById(R.id.address);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        showMyLocation();
    }

    public void showMyLocation() {
        if (googleMap != null) {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION};
            if (hasPermissions(this, permissions)) {
                googleMap.setMyLocationEnabled(true);
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                addMarkerAndZoom(location, getString(R.string.my_location), 15);
                            }
                        });
            } else {
                ActivityCompat.requestPermissions(this, permissions, ACCESS_LOCATION_PERMISSION_CODE);
            }
        }
    }

    public static boolean hasPermissions( Context context, String[] permissions )
    {
        for ( String permission : permissions )
        {
            if ( ContextCompat.checkSelfPermission( context, permission ) == PackageManager.PERMISSION_DENIED )
            {
                return false;
            }
        }
        return true;
    }

    public void addMarkerAndZoom( Location location, String title, int zoom  )
    {
        LatLng myLocation = new LatLng( location.getLatitude(), location.getLongitude() );
        googleMap.addMarker( new MarkerOptions().position( myLocation ).title( title ) );
        googleMap.moveCamera( CameraUpdateFactory.newLatLngZoom( myLocation, zoom ) );
    }

    public void onFindAddressClicked( View view )
    {
        startFetchAddressIntentService();
    }
    public void startFetchAddressIntentService() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        AddressResultReceiver addressResultReceiver = new AddressResultReceiver(new Handler(Looper.getMainLooper()));
                        addressResultReceiver.setAddressResultListener(address -> runOnUiThread(() -> {
                            MapsActivity.this.address.setText(address);
                            MapsActivity.this.address.setVisibility(View.VISIBLE);
                        }));
                        Intent intent = new Intent(MapsActivity.this, FetchAddressIntentService.class);
                        intent.putExtra(FetchAddressIntentService.RECEIVER, addressResultReceiver);
                        intent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, location);
                        startService(intent);
                    }
                });
    }

    public void onAddLocationClicked(View view) {
        Intent intent = new Intent(this, AddLocationActivity.class);
        startActivityForResult(intent, ADD_LOCATION_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_LOCATION_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                MyLocation result = (MyLocation) data.getExtras().get(getString(R.string.response));
                Location location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(result.getLatitude());
                location.setLongitude(result.getLongitude());
                addMarkerAndZoom(location, String.format("%s: %s", result.getName(), result.getDescription()), 15);
            }
        }
    }
}