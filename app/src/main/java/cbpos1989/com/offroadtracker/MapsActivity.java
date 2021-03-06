package cbpos1989.com.offroadtracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.ArrayList;



//import com.google.android.gms.location.LocationListener;

/**
 * Created by Alex Scanlan & Colm O'Sullivan on 28/09/2015.
 */
public class MapsActivity extends FragmentActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapLongClickListener {

    protected GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private ArrayList<Location> points = new ArrayList<Location>();
    private Polyline route;
    private Polyline liveRoute;

    private final String filename = "route.gpx";
    private boolean startStopLoc = false;
    static  boolean firstCoord = true;
    private SharedPreferences mPrefs;
    private static LatLng prevCoordinates;
    private MapsActivity thisActivity = this;

    File routeFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

//        try {
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 2, this);
//        } catch (SecurityException se) {
//            se.printStackTrace();
//        }

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        setUpMapIfNeeded();

       // SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
       // firstCoord = sharedPreferences.getBoolean("first_coord", true);
        Log.i("drawLine","Value of firstCoord" + firstCoord);

        try {
            Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocationGPS != null) {

                prevCoordinates = new LatLng(lastKnownLocationGPS.getLatitude(), lastKnownLocationGPS.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(prevCoordinates, 18));
                //onLocationChanged(lastKnownLocationGPS);
            }
        }catch(SecurityException se){
            se.printStackTrace();
        }

        points.clear();
        routeFile = new File(this.getFilesDir(), filename);
        GPXReader gpxReader = new GPXReader(this);
        gpxReader.readPath(routeFile);
        //routeFile.deleteOnExit();

        ArrayList<LatLng> polylinePoints = (ArrayList<LatLng>) gpxReader.getPoints();
        if(polylinePoints.size() > 1){
            prevCoordinates = polylinePoints.get(0);
            for(int i = 0; i < polylinePoints.size();++i){
                Log.i("Array Sizes", i + "Reader Array: " + gpxReader.getPoints().size() + " Polyline Array: " + polylinePoints.size());

                drawLine(polylinePoints.get(i));


            }
            mMap.addMarker(new MarkerOptions().position(polylinePoints.get(polylinePoints.size() -1)).title("Marker"));
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onDestroy(){
        GPXWriter gpxFile = new GPXWriter(this);
        try {
            routeFile = new File(this.getFilesDir(),filename);
            routeFile.createNewFile();
            gpxFile.writePath(routeFile, "GPX_Route", points);
            route.remove();

            if(route != null) {
                route.remove();
            }
            Log.i("WritingFile","Finished writing" + filename);

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            //SharedPreferences.Editor editor = sharedPreferences.edit();
            //editor.putBoolean("first_coord", firstCoord);
           // editor.commit();
            Log.i("drawLine","Value of commited firstCoord: " + firstCoord);
        }

        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if(startStopLoc) {
            Toast.makeText(this, "IN STOP", Toast.LENGTH_SHORT).show();

            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }

        super.onDestroy();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if(mLocation != null) {
            mLocation = location;
        }

        LatLng latLng = new LatLng(latitude, longitude);

        mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));

        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));

        points.add(location);
        Log.i("onLocationChanged", "Reached onLocationChanged before drawLine()");
        //Toast.makeText(this, "IN ONLOCCHANGED", Toast.LENGTH_SHORT).show();
        drawLine(location);
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    private void drawLine(Location location) {
        LatLng currCoordinates = new LatLng(location.getLatitude(),location.getLongitude());

        //Toast.makeText(this, prevCoordinates.toString() + "//// " + currCoordinates.toString(),Toast.LENGTH_SHORT).show();

        if(firstCoord){
            Log.i("drawLine", "Reached drawLine if statement");
            mMap.addMarker(new MarkerOptions().position(currCoordinates).title("Marker"));
            prevCoordinates = currCoordinates;
            firstCoord = false;
        }


        liveRoute = mMap.addPolyline(new PolylineOptions().geodesic(true)
                        .add(prevCoordinates)
                        .add(currCoordinates)
        );

        liveRoute.setColor(Color.RED);
        liveRoute.setWidth(5.0F);
        Log.i("Route Drawing", "Drawing from " + prevCoordinates + " to " + currCoordinates);
        prevCoordinates = currCoordinates;
    }

    private void drawLine(LatLng latlng) {

        LatLng currCoordinates = latlng;


        //Toast.makeText(this, prevCoordinates.toString() + "//// " + currCoordinates.toString(),Toast.LENGTH_SHORT).show();


        route = mMap.addPolyline(new PolylineOptions().geodesic(true)
                        .add(prevCoordinates)
                        .add(currCoordinates)
        );

        route.setColor(Color.RED);
        route.setWidth(5.0F);
        Log.i("Route Drawing", "Drawing from " + prevCoordinates + " to " + currCoordinates);
        prevCoordinates = currCoordinates;


    }

    public void stopLocationListener(View view) {
        //---------------CRAZY CODE BELOW THIS POINT-----------------------

        // Init the dialog object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("Enter description");

        // Set up the input

        //LayoutInflater factory = LayoutInflater.from(getApplicationContext());

        //final View v = factory.inflate(R.layout.dialog_layout, null);
        //final EditText input = (EditText) v.findViewById(R.id.dialog_edit_text);
        //final RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.radio_group_dialog);
        //input.setInputType(InputType.TYPE_CLASS_TEXT);

        //builder.setView(v);

        //builder.setTitle("");

        // Set up the buttons
        builder.setPositiveButton("Start/Stop", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(getApplicationContext(), "in method", Toast.LENGTH_SHORT).show();

                LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

                ImageButton button = (ImageButton) findViewById(R.id.stopLocListenerBtn);

                if (startStopLoc) {
                    Toast.makeText(getApplicationContext(), "IN STOP", Toast.LENGTH_SHORT).show();

                    try {
                        locationManager.removeUpdates(thisActivity);
                    } catch (SecurityException se) {
                        se.printStackTrace();
                    }

                    button.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);

                    startStopLoc = !startStopLoc;
                } else {
                    Toast.makeText(getApplicationContext(), "IN START", Toast.LENGTH_SHORT).show();

                    try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, thisActivity);
                    } catch (SecurityException se) {
                        se.printStackTrace();
                    }

                    button.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);

                    startStopLoc = !startStopLoc;
                }

            }
        });
        builder.setNegativeButton("New Route", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//
//                if (route != null) {
//                    Toast.makeText(thisActivity, "Removed Route", Toast.LENGTH_SHORT).show();
//                    route.remove();
//                }
//                if (liveRoute != null) {
//                    Toast.makeText(thisActivity, "Removed LiveRoute", Toast.LENGTH_SHORT).show();
//                    liveRoute.remove();
//                }
//                File routeFile = new File(thisActivity.getFilesDir(), filename);
//                boolean routeDeleted = routeFile.delete();
//                Log.i("Deleted", "Route Deleted");
            }
        });

        builder.show();

    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
//        mMap.addMarker(new MarkerOptions().position(latLng).title(
//                latLng.toString()));

//        Toast.makeText(getApplicationContext(),
//                "New marker added@" + latLng.toString(), Toast.LENGTH_LONG)
//                .show();

        //---------------CRAZY CODE BELOW THIS POINT-----------------------

        // Init the dialog object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("Enter description");

        // Set up the input

        LayoutInflater factory = LayoutInflater.from(getApplicationContext());

        final View v = factory.inflate(R.layout.dialog_layout, null);
        final EditText input = (EditText) v.findViewById(R.id.dialog_edit_text);
        final RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.radio_group_dialog);
        //input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(v);

        //builder.set

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "in method", Toast.LENGTH_SHORT).show();

                String description;
                description = input.getText().toString();

                int bitmap = 0;

                RadioButton checkedButton = (RadioButton) v.findViewById(radioGroup.getCheckedRadioButtonId());

                switch (checkedButton.getId()) {
                    case R.id.interest_radio_button:
                        bitmap = R.drawable.ic_explore_white_48dp;
                        break;

                    case R.id.danger_radio_button:
                        bitmap = R.drawable.ic_close_white_48dp;
                        break;
                }


                //Bitmap bitmap = drawableToBitmap(checkedButton.getBackground());

                //checkedButton.getButtonDrawable()

                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(description)
                        .icon(BitmapDescriptorFactory.fromResource(bitmap)));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void goBack(View v){
        onBackPressed();
    }
}

/*

LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        ImageButton button = (ImageButton) findViewById(R.id.stopLocListenerBtn);

        if(startStopLoc){
            Toast.makeText(this, "IN STOP", Toast.LENGTH_SHORT).show();

            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException se) {
                se.printStackTrace();
            }

            button.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);

            startStopLoc = !startStopLoc;
        } else{
            Toast.makeText(this, "IN START", Toast.LENGTH_SHORT).show();

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, this);
            } catch (SecurityException se) {
                se.printStackTrace();
            }

            button.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);

            startStopLoc = !startStopLoc;
           }
 */