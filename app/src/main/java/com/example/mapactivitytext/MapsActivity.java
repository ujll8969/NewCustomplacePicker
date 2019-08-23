package com.example.mapactivitytext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    private LatLng latLong;
    private Marker markerClicked;
    Geocoder geocoder;
    String apikey = "your APIKEY";
    FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    AutoCompleteTextView autoCompleteTextView;
    AutoCompleteAdapter adapter;
    PlacesClient placesClient;
    String mapSelectedAddress;
    String titleSelectedAddress;
    Button clear;
    View mapView;


    //Make sure put your own api key from google developer console..//




    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        clear = findViewById(R.id.clear);
        clear.setVisibility(View.INVISIBLE);
        geocoder = new Geocoder(this, Locale.getDefault());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapView = mapFragment.getView();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (apikey.isEmpty()) {
            Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apikey);
        }

        placesClient = Places.createClient(this);
        initAutoCompleteTextView();

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 95);
        }


    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
                onLocationChanged(mLastLocation);
            }
        };
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("You are here");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //stop location updates
        if (mGoogleApiClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        onMapClicked();
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            locationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.remove();
                    }
                    mCurrLocationMarker = mMap.addMarker(markerOptions);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                }
            });
        }

    }

    public void onMapClicked() {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }
                latLong = point;
                List<Address> addresses = new ArrayList<>();
                try {

                    addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(addresses == null || addresses.size()==0){
                    Toast.makeText(getApplicationContext(), "Location Not found", Toast.LENGTH_LONG).show();
                    return;
                }
                android.location.Address address = addresses.get(0);

                if (address != null) {
                    mapSelectedAddress = address.getAddressLine(0);
                    titleSelectedAddress = address.getSubLocality();
                    Toast.makeText(getApplicationContext(), mapSelectedAddress, Toast.LENGTH_SHORT).show();
                }
                //place marker where user just clicked
                mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(point).title(titleSelectedAddress)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

            }


        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void initAutoCompleteTextView() {
        autoCompleteTextView = findViewById(R.id.auto);
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView.setOnItemClickListener(autocompleteClickListener);
        adapter = new AutoCompleteAdapter(this, placesClient);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    clear.setVisibility(View.VISIBLE);
                } else {
                    clear.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        autoCompleteTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.TYPE_TEXT_VARIATION_NORMAL) {
                    hideSoftKeyboard();
                }
                return false;
            }


        });


    }


    private AdapterView.OnItemClickListener autocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l) {
            try {
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }
                final AutocompletePrediction item = adapter.getItem(i);
                String placeID = null;
                if (item != null) {
                    placeID = item.getPlaceId();
                }
                List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS
                        , Place.Field.LAT_LNG);

                FetchPlaceRequest request = null;
                if (placeID != null) {
                    request = FetchPlaceRequest.builder(placeID, placeFields)
                            .build();
                }

                if (request != null) {
                    placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(FetchPlaceResponse task) {
                            Log.d("hello", task.getPlace().getName() + "\n" + task.getPlace().getAddress() + "\n" + task.getPlace().getLatLng());
                            hideSoftKeyboard();
                            LatLng latLng = new LatLng(Objects.requireNonNull(task.getPlace().getLatLng()).latitude, task.getPlace().getLatLng().longitude);
                            mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng));
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                            clear.setVisibility(View.VISIBLE);

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                            clear.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    public void clear(View view) {
        autoCompleteTextView.setText("");
        clear.setVisibility(View.GONE);
    }

    public void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (getCurrentFocus() != null) {
            hideSoftKeyboard();
        }
        return super.dispatchTouchEvent(ev);
    }

}