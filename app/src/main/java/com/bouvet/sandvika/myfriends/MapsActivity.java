package com.bouvet.sandvika.myfriends;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.bouvet.sandvika.myfriends.Dagger.App;
import com.bouvet.sandvika.myfriends.gcm.MyGcmListenerService;
import com.bouvet.sandvika.myfriends.http.MyFriendsRestService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback {


    @Inject
    Retrofit retrofit;
    @Inject
     SharedPreferences sharedPreferences;

    private MyFriendsRestService service;

    private GoogleApiClient googleApiClient;
    private BroadcastReceiver messageReceiver;


    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ((App) getApplication()).getContextComponent().inject(this);
        service = retrofit.create(MyFriendsRestService.class);
        //region Map init
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //endregion

        //region Google LocationServices Api connect
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        //endregion


        //endregion

        //region MessageReceiver init
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                System.out.println("Received broadcastmessage from Registration Intent");

                if(action == MyGcmListenerService.BroadCastRecieved) {
                    String key = intent.getStringExtra(MyGcmListenerService.BroadCastRecievedMessage);
                    showUserNearMessage(key);
                }
            }
        };

        //endregion



    }
    private void showUserNearMessage(String message) {

        Snackbar.make(findViewById(android.R.id.content),message, Snackbar.LENGTH_LONG)
                .show();

    }



    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        IntentFilter intentFilter = new IntentFilter(MyGcmListenerService.BroadCastRecieved);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);

        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

        super.onPause();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        tryToConfigureLocationUpdates();
    }

    private void tryToConfigureLocationUpdates() {
        if (googleMap != null && googleApiClient.isConnected()) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            } else {
                configureLocationUpdates();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        tryToConfigureLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != 0) {
            throw new RuntimeException("Unknown request code received");
        }
        for (String permission : permissions) {
            if (android.Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                configureLocationUpdates();
            }
        }
    }

    private void configureLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        this.googleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(final Location location) {
        String user = sharedPreferences.getString("userName",null);
        if (user != null) {

            Call<Void> locationChanged = service.updateLocation(user, new double[]{location.getLatitude(), location.getLongitude()});
            locationChanged.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    System.out.println("[onLocationChanged] success!!");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    System.out.println("[onLocationChanged] failure!!");
                }
            });
        }

    }
}
