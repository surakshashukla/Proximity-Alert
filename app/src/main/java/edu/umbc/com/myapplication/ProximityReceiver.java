package edu.umbc.com.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Title: Proximity Alert
 * Authors: Suraksha Shukla 
 * APIs enabled:
 *   Google Maps Android API
 *   Google Maps Geolocation API
 *   Google Places API for Android
 *   Google Places API Web Service
 */

public class ProximityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("Google Map", "Proximity receiver" );

        // Key for determining whether user is leaving or entering
        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        //Gives whether the user is entering or leaving in boolean form
        boolean state = intent.getBooleanExtra(key, false);

        //toast based on key when the receiver is called
        if(state){
            Log.i("MyTag", "You entered the area!");
            Toast.makeText(context, "You entered the area", Toast.LENGTH_LONG).show();
        }else{
            Log.i("MyTag", "You are exiting the area!!");
            Toast.makeText(context, "You are exiting the area!!", Toast.LENGTH_LONG).show();
        }
    }
}