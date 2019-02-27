package jp.ac.titech.itpro.sdl.machiawase;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, LocationSource, GoogleMap.OnMyLocationButtonClickListener

{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private DatabaseReference groupDatabaseReference;

    private String groupID;
    private String userName;
    private double mlat;
    private double mlng;
    private Set<Marker> markerSet = new HashSet<>();
    private boolean cameraPosition = true;
    int color = 0;

    private ArrayList<BitmapDescriptor> bitmapDescriptors = new ArrayList<>();


    private Map<String, Place> memberInfo = new HashMap<>();

    public static class Place {
        public double latitude;
        public double longtitude;

        public Place(double latitude, double longtitude) {
            this.latitude = latitude;
            this.longtitude = longtitude;
        }
    }


    private LocationSource.OnLocationChangedListener onLocationChangedListener = null;

    private int priority[] = {LocationRequest.PRIORITY_HIGH_ACCURACY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
            LocationRequest.PRIORITY_LOW_POWER, LocationRequest.PRIORITY_NO_POWER};
    private int locationPriority;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent i = getIntent();
        groupID = i.getStringExtra("GroupID");
        Log.v("MainActivityIntent", groupID);

        userName = i.getStringExtra("UserName");
        Log.v("MainActivityIntent", userName);


        groupDatabaseReference = Utills.getDatabase().getReference("/" + groupID);


        // LocationRequest を生成して精度、インターバルを設定
        locationRequest = LocationRequest.create();

        // 測位の精度、正確性の優先度
        locationPriority = priority[0];

        if (locationPriority == priority[0]) {
            // 位置情報の精度を優先する場合
            locationRequest.setPriority(locationPriority);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(16);
        } else if (locationPriority == priority[1]) {
            // 消費電力を考慮する場合
            locationRequest.setPriority(locationPriority);
            locationRequest.setInterval(60000);
            locationRequest.setFastestInterval(16);
        } else if (locationPriority == priority[2]) {
            // "city" level accuracy
            locationRequest.setPriority(locationPriority);
        } else {
            // 外部からのトリガーでの測位のみ
            locationRequest.setPriority(locationPriority);
        }

        //SupportMapFragmentを使う
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        bitmapDescriptors.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));


    }

    @Override
    protected void onStart() {
        super.onStart();
        groupDatabaseReference.addChildEventListener(
                new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.v("MapsActivityOnChildAdded", dataSnapshot.getKey().toString());
                        Log.v("MapsActivityOnChildAdded", dataSnapshot.child("latitude").getValue().toString());
                        Log.v("MapsActivityOnChildAdded", dataSnapshot.child("longtitude").getValue().toString());
                        if (!dataSnapshot.getKey().toString().equals(userName)) {
                            memberInfo.put(dataSnapshot.getKey().toString(), new Place(
                                    Double.valueOf(dataSnapshot.child("latitude").getValue().toString()),
                                    Double.valueOf(dataSnapshot.child("longtitude").getValue().toString())
                            ));
                            updateUI(dataSnapshot.getKey().toString());
                        }

                        //memberInfo.put(.getKey(),new Place(Double.valueOf(dataSnapshot.child("latitude").getValue().toString()),Double.valueOf(dataSnapshot.child("longtitude").getValue().toString())));
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        Log.v("MapsActivityOnChildChanged", dataSnapshot.getKey().toString());
                        Log.v("MapsActivityOnChildChanged", dataSnapshot.child("latitude").getValue().toString());
                        Log.v("MapsActivityOnChildChanged", dataSnapshot.child("longtitude").getValue().toString());

                        if (!dataSnapshot.getKey().toString().equals(userName)) {
                            memberInfo.put(dataSnapshot.getKey().toString(), new Place(
                                    Double.valueOf(dataSnapshot.child("latitude").getValue().toString()),
                                    Double.valueOf(dataSnapshot.child("longtitude").getValue().toString())
                            ));
                            updateUI(dataSnapshot.getKey().toString());
                        }


                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.v("MapsActivityOnChildRemoved", dataSnapshot.getKey().toString());
                        Log.v("MapsActivityOnChildRemoved", dataSnapshot.child("latitude").getValue().toString());
                        Log.v("MapsActivityOnChildRemoved", dataSnapshot.child("longtitude").getValue().toString());

                        if (!dataSnapshot.getKey().toString().equals(userName)) {
                            memberInfo.remove(dataSnapshot.getKey().toString());
                            removeUI(dataSnapshot.getKey().toString());
                        }

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                        Log.v("MapsActivityOnChildMoved", dataSnapshot.getKey().toString());
                        Log.v("MapsActivityOnChildMoved", dataSnapshot.child("latitude").getValue().toString());
                        Log.v("MapsActivityOnChildMoved", dataSnapshot.child("longtitude").getValue().toString());

                        memberInfo.remove(dataSnapshot.getKey().toString());

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );
    }

    // onResumeフェーズに入ったら接続
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    // onPauseで切断
    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("debug", "permission granted");

            mMap = googleMap;
            // default の LocationSource から自前のsourceに変更する
            mMap.setLocationSource(this);
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
            mMap.moveCamera(CameraUpdateFactory.zoomTo(20f));
        } else {
            Log.d("debug", "permission error");
            return;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // check permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("debug", "permission granted");

            // FusedLocationApi
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, locationRequest, this);
        } else {
            Log.d("debug", "permission error");
            return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("debug", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("debug", "onConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("debug", "onLocationChanged");
        if (onLocationChangedListener != null) {
            onLocationChangedListener.onLocationChanged(location);

            mlat = location.getLatitude();
            mlng = location.getLongitude();

            Log.d("debug", "location=" + mlat + "," + mlng);

            //Toast.makeText(this, "location="+mlat+","+mlng, Toast.LENGTH_SHORT).show();

            // Add a marker and move the camera
            updateUI(userName);

            groupDatabaseReference.child(userName).setValue(new Place(mlat, mlng), new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Log.e("onLocationChanged", "Data could not be saved " + databaseError.getMessage());
                    } else {
                        Log.v("onLocationChanged", "Data saved successfully.");
                    }
                }
            });

        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        this.onLocationChangedListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        this.onLocationChangedListener = null;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "onMyLocationButtonClick", Toast.LENGTH_SHORT).show();
        return false;
    }


    public void updateUI(String name) {
        if (userName.equals(name)) {
            LatLng newLocation = new LatLng(mlat, mlng);
            for (Marker m : markerSet) {
                if (m.getTitle().equals("Your Location")) {
                    Log.v("gettitle", m.getTitle());
                    m.setPosition(newLocation);
                    if (cameraPosition == true) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLocation));
                        cameraPosition = false;
                    }
                    return;
                }
            }
            Marker mm = mMap.addMarker(new MarkerOptions().position(newLocation).title("Your Location"));
            mm.showInfoWindow();
            markerSet.add(mm);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newLocation));
            return;
        }

        for (Iterator<String> iterator = memberInfo.keySet().iterator(); iterator.hasNext(); ) {
            color = color % bitmapDescriptors.size();
            String key = iterator.next();
            Place p = memberInfo.get(key);
            for (Marker m : markerSet) {
                if (m.getTitle().equals(name)) {
                    m.setPosition(new LatLng(p.latitude, p.longtitude));
                    return;
                }
            }

            Marker mm = mMap.addMarker(new MarkerOptions().position(new LatLng(p.latitude, p.longtitude)).title(key).icon(bitmapDescriptors.get(color)));
            mm.showInfoWindow();
            markerSet.add(mm);
            color++;
        }
    }

    public void removeUI(String name) {
        for (Marker m : markerSet) {
            if (m.getTitle().equals(name)) {
                m.remove();
                markerSet.remove(m);
                return;
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        groupDatabaseReference.child(userName).removeValue();
    }
}
