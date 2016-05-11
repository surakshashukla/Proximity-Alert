package edu.umbc.com.myapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Title: Proximity Alert
 * Authors: Suraksha Shukla
 * APIs enabled:
 *   Google Maps Android API
 *   Google Maps Geolocation API
 *   Google Places API for Android
 *   Google Places API Web Service
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnConnectionFailedListener, LocationListener {
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private Marker placeMarker;
    private Circle placeCircle;
    private String TAG = "Google map"; //tag for debugging
    private PlaceAutocompleteFragment autocompleteFragment; //autocomplete fragment for map
    private  static int radius = 200; //200m radius around place selected
    private String ACTION_FILTER = "com.example.proximityalert";  //action filter for sending pending intent
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private ProximityReceiver proximityReceiver;
    private String provider; //gps or network provider
    private LatLng destinationLatLng; //place latitude and longitude
    private LatLng defaultLatLng = new LatLng(39.2551, -76.7112); // default location to UMBC's latitude longitude
    private PendingIntent pendingIntent; //pending intent for proximity alert
    private Handler myHandler = new Handler(); //handler for runnable
    private String preference_ = "PREF"; //for shared preference

    @Override
    protected void onCreate(Bundle savedInstanceState) {

       super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        }

        //force screen into Portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        //setContext view to maps activity
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //create new google api client
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .addApi(AppIndex.API).build();

        //connect to google server
        mGoogleApiClient.connect();

        //get the map
        mapFragment.getMapAsync(this);

        // create the ability for the user to autocomplete their place selection
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        }

        //get the Location Service
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //if you don't have a client, create it
        if(mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .addApi(AppIndex.API).build();
        }

        //if you aren't connected to the server, connect to the server
        if(mGoogleApiClient.isConnected() == false) {
            mGoogleApiClient.connect();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission Granted.

                } else {

                    // Permission Denied
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission Granted

                } else {

                    // Permission Denied
                }
                return;
            }
        }
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
        mMap = googleMap;

        // Must get the fine and coarse location in order to continue
        do
        {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }while(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);

        //let google get my location
        mMap.setMyLocationEnabled(true);

        //get location update every 3000 milliseconds
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 3000, 0, this);

        //set up the ability to store a location on device using sharedPreference
        SharedPreferences setLocation = getSharedPreferences(preference_, Context.MODE_PRIVATE);
        final SharedPreferences.Editor edit = setLocation.edit();

        //setup the autocomplete fragment
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        //Set Bounds Bias for Autocomplete Feature (To get closest places to your last location)
        Location currentKnownLocation2 = locationManager.getLastKnownLocation(provider);
        LatLng latLng = new LatLng(currentKnownLocation2.getLatitude(), currentKnownLocation2.getLongitude());
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(latLng);
        LatLngBounds bounds = builder.build();
        autocompleteFragment.setBoundsBias(bounds);


        //when the user searches and selects a place, do this
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                //let google get my location
                mMap.setMyLocationEnabled(true);

                //get details of the place selected
                String placeDetailsStr = place.getName() + "\n"
                        + place.getId() + "\n"
                        + place.getLatLng().toString() + "\n"
                        + place.getAddress() + "\n"
                        + place.getAttributions();

                Log.d(TAG, "Place: " + place.getLatLng());

                //getting latitude and longitude from the selected place in autocomplete
                destinationLatLng = place.getLatLng();

                //if there is already a circle on the map, get rid of it
                if (placeCircle != null) {
                    placeCircle.remove();
                }
                //add radius circle around place the user selected
                placeCircle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(destinationLatLng.latitude, destinationLatLng.longitude))
                        .radius(radius)
                        .strokeColor(Color.RED));

                //if there is already a place marker, delete it
                if (placeMarker != null) {
                    placeMarker.remove();
                }
                //add the new place marker to the map
                placeMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title(place.getName().toString()));

                //clear, then add the new place marker info to shared preferences
                edit.clear();
                edit.putString("Latitude", new Double(destinationLatLng.latitude).toString());
                edit.putString("Longitude", new Double(destinationLatLng.longitude).toString());
                edit.putString("Name", place.getName().toString());
                edit.commit();

                //get the best provider: gps or network
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                provider = locationManager.getBestProvider(criteria, true);

                //Move camera to view your location and the place you selected in the same view
                Location currentKnownLocation1 = locationManager.getLastKnownLocation(provider);
                LatLng latLng = new LatLng(currentKnownLocation1.getLatitude(), currentKnownLocation1.getLongitude());
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(latLng);
                builder.include(place.getLatLng());
                int width = getResources().getDisplayMetrics().widthPixels;
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width + 10, width + 10, 150));

                //latLng is the place selected
                latLng = destinationLatLng;

                //Since you selected a new place, setup a new proximity alert
                proximityReceiver = new ProximityReceiver();
                registerReceiver(proximityReceiver, new IntentFilter(ACTION_FILTER));
                Intent proximityIntent = new Intent(ACTION_FILTER);

                //check if you already have a pending intent
                boolean result = checkIfPendingIntentIsRegistered();
                if(result)
                {
                    //You already have a pending intent, so cancel it
                    PendingIntent.getBroadcast(getApplicationContext(), -1, proximityIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT).cancel();
                    Log.d(TAG, "TAG: PendingIntentCancel:OnMapReady1");
                }

                // Creating a pending intent which will be invoked by LocationManager when the specified region is
                // entered or exited
                Log.d("ERROR TAG", "TAG: Proximity Alert is added" + latLng);
                pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), -1, proximityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                /* Setting proximity alert
                   The pending intent will be invoked when the device enters or exits the region 20 meters
                   away from the marked point
                   The -1 indicates that, the monitor will not be expired */
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.addProximityAlert(latLng.latitude, latLng.longitude, radius, -1, pendingIntent);

            }

            // If there is an error, print it to the log
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.d(TAG, "An error occurred: " + status);
            }
        });


        //When the app is reopened, the last place selected needs to be placed on the screen

        //If the user has previously selected a place, then the below code will run when the map is created
        SharedPreferences getLocation = getSharedPreferences(preference_, Context.MODE_PRIVATE);

        //get latitude, longitude, and name from shared preferences, if not there, print "0"
        String latitude = getLocation.getString("Latitude", "0");
        String longitude = getLocation.getString("Longitude", "0");
        String name = getLocation.getString("Name", "0");

        //Check for a saved place
        if( latitude != "0") {
            //Yes, a place is saved, so set up the proximity alert for that saved place
            LatLng lastLatLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

            //if placeMarker exists, delete it
            if( placeMarker != null)
            {
                placeMarker.remove();
            }
            //add the place marker for the new location
            placeMarker = mMap.addMarker(new MarkerOptions().position(lastLatLng).title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));


            //if circle around place exists, delete it
            if(placeCircle != null) {
                placeCircle.remove();
            }
            placeCircle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(lastLatLng.latitude, lastLatLng.longitude))
                    .radius(radius)
                    .strokeColor(Color.RED));
            

            //get the best location provider
            criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            provider = locationManager.getBestProvider(criteria, true);
            locationManager.requestLocationUpdates(provider, 3000, 0, this);

            //get the last known/current user location from the gps or network provider
            Location currentKnownLocation = locationManager.getLastKnownLocation(provider);
            LatLng currentLatLng;

            //if you have a current location, use it for the map display
            if(locationManager.getLastKnownLocation(provider) != null) {
                currentLatLng = new LatLng(currentKnownLocation.getLatitude(), currentKnownLocation.getLongitude());
                builder = new LatLngBounds.Builder();
                builder.include(currentLatLng);
                bounds = builder.build();
                int width = getResources().getDisplayMetrics().widthPixels;
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width + 10, width + 10, 150));
                //update map to view user and place
            }
            else
            {
                //default to UMBC coordinates
                currentLatLng = defaultLatLng;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 9));
            }
        }
        else
        {
            //default to UMBC coordinates
            LatLng currentLatLng = defaultLatLng;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 9));

        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {
        //when the user changes location, call the location runnable thread
        // It updates the map's view to where the user is now located
        LocWork tmp  = new LocWork(location);   //or use myHandler.post(new LocWork(location));
        myHandler.post(tmp);

    }

    //check if there is already a pending intent
    private boolean checkIfPendingIntentIsRegistered() {
        Intent intent = new Intent(ACTION_FILTER);
        // Build the exact same pending intent you want to check.
        // Everything has to match except extras.
        return (PendingIntent.getBroadcast(getApplicationContext(), -1, intent, PendingIntent.FLAG_CANCEL_CURRENT) != null);
    }


    @Override
    public void onStart() {
        super.onStart();

        //make sure fine and coarse location settings are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        // if not connected to server, connect
        if(mGoogleApiClient.isConnected() == false) {
            mGoogleApiClient.connect();
        }

        //get view of map
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://edu.umbc.com.myapplication/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);


        //When the app is reopened, the last place selected needs to have the proximity alert reset

        //If the user has previously selected a place, then the below code will run when the map is created
        SharedPreferences getLocation = getSharedPreferences(preference_, Context.MODE_PRIVATE);

        //get latitude, longitude from shared preferences, if not there, print "0"
        String latitude = getLocation.getString("Latitude", "0");
        String longitude = getLocation.getString("Longitude", "0");

        //Check for a saved place
        if( latitude != "0") {
            //Yes, a place is saved, so set up the proximity alert for that saved place
            LatLng lastLatLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

            // set up proximity alert
            proximityReceiver = new ProximityReceiver();
            registerReceiver(proximityReceiver, new IntentFilter(ACTION_FILTER));
            Intent proximityIntent = new Intent(ACTION_FILTER);

            //check for pending intent. if exists, cancel it
            boolean result = checkIfPendingIntentIsRegistered();
            if (result) {
                //Pending intent already exists, so cancel it
                PendingIntent.getBroadcast(getApplicationContext(), -1, proximityIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT).cancel();
                Log.d(TAG, "PendingIntentCancel:OnStart");
            }


            // Creating a pending intent which will be invoked by LocationManager when the specified region is
            // entered or exited
            Log.d("ERROR TAG", "TAG: OnStart: Proximity Alert is added" + lastLatLng);
            pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), -1, proximityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            //add the proximity alert
            locationManager.addProximityAlert(lastLatLng.latitude, lastLatLng.longitude, radius, -1, pendingIntent);

            //get the best location provider
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            provider = locationManager.getBestProvider(criteria, true);
            locationManager.requestLocationUpdates(provider, 3000, 0, this);

        }
    }

    //Class for updating the location and map
    private class LocWork implements Runnable {

        private Location location_;

        public LocWork(Location location) {  //using constructor to access the location
            location_ = location;
        }

        @Override
        public void run() {

            //make sure that fine and coarse locations are granted
            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            }

            //Prefer to use gps (fine) location, but will use network (coarse) location if needed
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            provider = locationManager.getBestProvider(criteria, true);

            //Get your current/last known location
            Location currentKnownLocation = locationManager.getLastKnownLocation(provider);
            LatLng latLng = new LatLng(currentKnownLocation.getLatitude(), currentKnownLocation.getLongitude());

            //if you have selected a place, then camera view should be of the user and the place
            if(placeMarker != null) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(latLng);
                builder.include(placeMarker.getPosition());
                int width = getResources().getDisplayMetrics().widthPixels;
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width + 10, width + 10, 150));
            }
            else
            {
                //the camera view is of the user
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();


        //on app stop, unregister the proximity receiver
        if(proximityReceiver != null) {
            unregisterReceiver(proximityReceiver);
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //Get rid of the connection to the website
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://edu.umbc.com.myapplication/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);

        //get rid of the connection to the server
        mGoogleApiClient.disconnect();

        //Remove any pending intent
        Intent proximityIntent = new Intent(ACTION_FILTER);
        boolean result = checkIfPendingIntentIsRegistered();
        if(result)
        {
            PendingIntent.getBroadcast(getApplicationContext(), -1, proximityIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT).cancel();
            Log.d(TAG, "TAG: PendingIntentCancel:OnStop");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
