package com.example.navigation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.RequestClientOptions;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;

import java.util.List;

//OnMapReadyCallback interface allows us to know when the map view has finished initialising and ready to go
//OnMapReadyCallback interface allows us to know when the map view has finished initialising and ready to go

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    //variable to store current location
    private Point originPosition;
    private Button startButton;
    private Point destinationPosition;
    private Location originLocation;
    private Marker destinationMarker;
    private NavigationMapRoute navigationMapRoute;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));

        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.map_view);
        startButton = findViewById(R.id.startButton);
        //Map view contains its own lifecycle methods for managing open GL lifecycle
        //In order for our App to correctly call the MapView lifecycle methods we need to override the activity lifecycle methods and add the mapview
        //Corresponding lifecycle method.

        mapView.onCreate(savedInstanceState);
        //set the callback
        mapView.getMapAsync(this);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Launch Navigation UI

            }
        });

    }

    //onMapReady is offCourse part of onMapReady callback
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        //setting the Listener
        map.addOnMapClickListener(this);
        enableLocation();

    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            //do some valuable stuff
            initializeLocationsEngine();
            initializeLocationLayer();
        }
        //if permission is not granted
        else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);

        }

    }

    //already checked permissions
    @SuppressWarnings("MissingPermission")
    private void initializeLocationsEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        //setting priority
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        //activating the engine
        locationEngine.activate();
        //if there was a last location then we have to get access to it
        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            permissionsManager = new PermissionsManager(this);

            locationEngine.addLocationEngineListener(this);

        }

    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer() {

        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        //responsible to show or hide the location icon and camera tracking location
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);//as per changes in user location camera will track the location
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS);
        locationLayerPlugin.setLocationLayerEnabled(true);

    }

    //To make sure that camera move to our current location and zoom
    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 13.0));
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
//dropping a marker wherever user tab
        destinationMarker = map.addMarker(new MarkerOptions().position(point));
        //setting destination position
        destinationPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());

        startButton.setEnabled(true);
        startButton.setBackgroundResource(R.color.mapbox_blue);
    }

    //onConnected and onLocationChanged are part of LocationEngine Listener
    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {

        //requesting location updates
        locationEngine.requestLocationUpdates();

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            originLocation = location;
            setCameraPosition(location);
        }

    }

    //onExplanationNeeded and onPermissionResult are part of Permisssions Listner
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //if permission is not granted then present a dialog
        Toast.makeText(this, "Why you want to aceess", Toast.LENGTH_SHORT).show();


    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation();
        }

    }

    //takes care all of the permissions stuff
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    //to avoid any memory leak
    @Override
    @SuppressWarnings("MissingPermission")
    protected void onPause() {
        super.onPause();

        mapView.onPause();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }


}