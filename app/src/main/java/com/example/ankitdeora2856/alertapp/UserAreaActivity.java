package com.example.ankitdeora2856.alertapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.tonikamitv.loginregister.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class UserAreaActivity extends AppCompatActivity
        implements GooglePlayServicesClient.ConnectionCallbacks,
        com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.OnConnectionFailedListener {

    String myJSON;
    String JSON_STRING;
    String myRoutes;
    String ROUTE_STRING;
    private static final String TAG_BID = "busId";
    private static final String TAG_LAT = "busLatitude";
    private static final String TAG_LONG = "busLongitude";
    private static final String TAG_SPEED = "busSpeed";
    private static final String TAG_CONTACT = "busContact";
    private static final String TAG_NUMBER  = "busNumber";
    private static final String TAG_RESULTS = "result";
    private static final String TAG_DISTANCE = "distance";
    private static final String TAG_MINUTES = "minutes";

    private static final String TAG_BusID = "BusId";
    private static final String TAG_BusNumber = "BusNumber";
    private static final String TAG_LONGITUDES = "RouteLongitudes";
    private static final String TAG_LATITUDES = "RouteLatitudes";
    private static final String TAG_NAMES = "RouteNames";

    JSONArray events = null;
    ArrayList<HashMap<String, String>> eventList;
    ListView list;
    ListAdapter myAdapter;
    ProgressDialog pDialog;

    private GoogleMap myMap;            // map reference
    private Location prevLoc = null;
    private LocationClient myLocationClient;

    private Handler handler;
    private Runnable runnableCode;
    private boolean onceUpdated;
    private ImageView arrow;

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(10000)         // 10 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private static final String TAG = "UserAreaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sliding_main);

        getMapReference();
        final int b_id = getIntent().getExtras().getInt("b_id");

        //setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        //getData();

        onceUpdated = false;
        // Create the Handler object (on the main thread by default)
        handler = new Handler();
        // Define the code block to be executed

        final Response.Listener<String> driverResponseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.getBoolean("success");
                    if (!success) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(UserAreaActivity.this);
                        builder.setMessage("Updation Failed")
                                .setNegativeButton("Retry", null)
                                .create()
                                .show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                Log.d("Handlers", "Called on main thread");
                // Repeat this the same runnable code block again another 2 seconds
                if(onceUpdated){
                    DatabaseRequest databaseRequest = new DatabaseRequest(prevLoc.getLatitude(), prevLoc.getLongitude(), prevLoc.getSpeed(), b_id, driverResponseListener);
                    RequestQueue queue = Volley.newRequestQueue(UserAreaActivity.this);
                    queue.add(databaseRequest);
                }

                //Toast.makeText(UserAreaActivity.this,"2 seconds over...", Toast.LENGTH_SHORT).show();
                handler.postDelayed(runnableCode, 2000);
            }
        };
// Start the initial runnable task by posting through the handler
        //handler.postDelayed(runnableCode,2000);
        handler.post(runnableCode);

    }


    @Override
    public void onLocationChanged(Location location) {
        onceUpdated = true;
        if(prevLoc==null){
            prevLoc = location;
        }

        double distance = location.distanceTo(prevLoc);
        //Toast.makeText(UserAreaActivity.this,"location changed..."+distance, Toast.LENGTH_SHORT).show();
        if(distance>1){
            prevLoc = location;
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        myLocationClient.requestLocationUpdates(
                REQUEST,
                this); // LocationListener

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     *     Activity's lifecycle event.
     *     onResume will be called when the Activity receives focus
     *     and is visible
     */
    @Override
    protected  void onResume(){
        handler.post(runnableCode);
        super.onResume();
        getMapReference();
        wakeUpLocationClient();
        myLocationClient.connect();

    }

    /**
     *      Activity's lifecycle event.
     *      onPause will be called when activity is going into the background,
     */
    @Override
    public void onPause(){
        handler.removeCallbacks(runnableCode);
        super.onPause();
        if(myLocationClient != null){
            myLocationClient.disconnect();
        }
    }

    /**
     *
     * @param lat - latitude of the location to move the camera to
     * @param lng - longitude of the location to move the camera to
     *            Prepares a CameraUpdate object to be used with  callbacks
     */
    private void gotoMyLocation(double lat, double lng) {
        changeCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(lat, lng))
                        .zoom(15.5f)
                        .bearing(0)
                        .tilt(25)
                        .build()
        ), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                // Your code here to do something after the Map is rendered
            }

            @Override
            public void onCancel() {
                // Your code here to do something after the Map rendering is cancelled
            }
        });
    }

    /**
     *      When we receive focus, we need to get back our LocationClient
     *      Creates a new LocationClient object if there is none
     */
    private void wakeUpLocationClient() {
        if(myLocationClient == null){
            myLocationClient = new LocationClient(getApplicationContext(),
                    this,       // Connection Callbacks
                    this);      // OnConnectionFailedListener
        }
    }

    /**
     *      Get a map object reference if none exits and enable blue arrow icon on map
     */
    private void getMapReference() {
        if(myMap == null){
            myMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
        if(myMap != null){
            myMap.setMyLocationEnabled(true);
        }

    }

    private void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
        myMap.moveCamera(update);
    }


    protected double roundTwoDecimals(double d) {
        DecimalFormat twoDecimals = new DecimalFormat("#.##");
        return Double.valueOf(twoDecimals.format(d));
    }

}
