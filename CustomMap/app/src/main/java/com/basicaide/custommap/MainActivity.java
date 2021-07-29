package com.basicaide.custommap;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,ActivityCompat.OnRequestPermissionsResultCallback {

    //String url = "https://jalaepe.000webhostapp.com/markers.json";
    String url = "https://jalaepe.github.io/Mapdata/markers.json";
    List<MarkersData> data;
    private GoogleMap mMap;
    boolean online = false;
    FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    int idx=1;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait ....");
        progressDialog.setCancelable(false);
        getData();
    }
    @Override
    public void onMapReady(GoogleMap p1) {
        mMap = p1;
        enableMyLocation();
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }
            @Override
            public View getInfoContents(Marker marker) { // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.infowindow,null, false);
                TextView title = infoWindow.findViewById(R.id.tvTitle);
                title.setText(marker.getTitle());
                TextView snippet = infoWindow.findViewById(R.id.tvSnippet);
                snippet.setText(marker.getSnippet());
                return infoWindow;
            }
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(data.get(0).lat, data.get(0).lon)));
        for(int i = 0 ; i < data.size() ; i++) {
            MarkerOptions mo = new MarkerOptions()
                    .title(data.get(i).title)
                    .snippet(data.get(i).snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_BLUE))
                    .position(new LatLng(data.get(i).lat, data.get(i).lon));
            mMap.addMarker(mo);
           // mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng p1)
            {
                MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(p1.latitude, p1.longitude))
                        .title("YourNew Location "+idx++ +"\n\n" +"Latitude "+ p1.latitude + "\n\n" + "Longitude " + p1.longitude)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN));
                mMap.addMarker(marker);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("location","Latitude : "+p1.latitude+"\nLongitude : "+p1.longitude);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(MainActivity.this,clipData+"\nhas been copied.",Toast.LENGTH_LONG).show();
                dialogBox("If you want to add your new location, please contact to ADMIN"+"\n\n"+"Your New Location is "+"\n\n"+
                        "Latitude :"+p1.latitude+"\n"+"Longitude :"+p1.longitude);
            }
        });
    }
    private void getData() {
        if (isOnline()) {
            online = true;
            progressDialog.show();
            new DownloadTask().execute(url);
        } else {
            SharedPreferences spf = getSharedPreferences("MapData", Context.MODE_PRIVATE);
            String lastdata = spf.getString("LastData", "");
            try {
                processJSON(lastdata);
            } catch (Exception e) { }
        }
    }
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... p1) {
            String result = JSONDownloader.download(p1[0]);
            return result;
        }
        @Override
        public void onPostExecute(String result) {
            processJSON(result);
            SharedPreferences spf = getSharedPreferences("MapData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = spf.edit();
            editor.putString("LastData", result);
            editor.apply();
            progressDialog.dismiss();
        }
    }
        private void processJSON(String result){
                data=new ArrayList<MarkersData>();
                try {
                JSONObject jo=new JSONObject(result);
                JSONArray jarr=jo.getJSONArray("mapdata");
                for(int i=0;i<jarr.length();i++){
                    jo=jarr.getJSONObject(i);
                    MarkersData item=new MarkersData(jo.get("title").toString(),
                            jo.get("snippet").toString(),jo.getDouble("lat"),jo.getDouble("lon"),jo.get("city").toString());
                    data.add(item);
                }
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MainActivity.this);
            }
            catch (Exception e)
            {
                //Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;
        }
    }
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }
    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add("Refresh").setIcon(R.drawable.ic_action_name).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Data List");
        menu.add("Map View");
        menu.add("Satellite View");
        menu.add("About");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getTitle().equals("Refresh")){
            getData();
        }
        if(item.getTitle().equals("Data List")){
            startActivity(new Intent(MainActivity.this, DataListActivity.class));
            finish();
        }
        if(item.getTitle().equals("Map View")){

            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if(item.getTitle().equals("Satellite View")){

            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if(item.getTitle().equals("About")){
            dialogBox("This Application is created by U Lay Gyi.\nAnyone can donate to +959972381187.");
        }
        return super.onOptionsItemSelected(item);
    }
    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }
    public void dialogBox(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                        //Do anything
                        Toast.makeText(MainActivity.this,"Thank You",Toast.LENGTH_LONG).show();
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}

