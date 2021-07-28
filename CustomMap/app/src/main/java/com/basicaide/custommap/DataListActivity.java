package com.basicaide.custommap;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DataListActivity extends AppCompatActivity {

    MyAdapter adapter;
    ListView lv;
    List<MarkersData> data,searchdata;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        lv=findViewById(R.id.lv);
        getData();
        adapter = new MyAdapter();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialogBox(searchdata.get(position).title,searchdata.get(position).snippet,searchdata.get(position).lat,searchdata.get(position).lon);
            }
        });
    }
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
       getMenuInflater().inflate(R.menu.search_menu,menu);
       MenuItem myActionMenuItem = menu.findItem(R.id.menu_search);
       SearchView sv = (SearchView) myActionMenuItem.getActionView();
       sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
           @Override
           public boolean onQueryTextSubmit(String p1)
           {
               return false;
           }
           @Override
           public boolean onQueryTextChange(String p1)
           {
              try{ adapter.filter(p1);}catch (Exception e){}

               return false;
           }
       });
       return super.onCreateOptionsMenu(menu);
   }
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return (super.onOptionsItemSelected(menuItem));
    }
    private void getData() {
        SharedPreferences spf = getSharedPreferences("MapData", Context.MODE_PRIVATE);
        String lastdata = spf.getString("LastData", "");
        if(lastdata.length()>0){
            try {
                processJSON(lastdata);
            } catch (Exception e) {
            }
        }else{
            Toast.makeText(DataListActivity.this,"No data at this time.",Toast.LENGTH_LONG).show();
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
            lv.setAdapter(new MyAdapter());
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }
    class MyAdapter extends BaseAdapter
    {
        public MyAdapter(){
            searchdata=new ArrayList<>();
            searchdata.addAll(data);
        }
        @Override
        public int getCount()
        {
// TODO: Implement this method
            return searchdata.size();
        }
        @Override
        public Object getItem(int p1)
        {
// TODO: Implement this method
            return searchdata.get(p1);
        }
        @Override
        public long getItemId(int p1)
        {
// TODO: Implement this method
            return 0;
        }
        @Override
        public View getView(int p1, View p2, ViewGroup p3)
        {
            if(p2 == null) {
                p2 = getLayoutInflater().inflate(R.layout.item_layout, null);
            }
            TextView tv1=(TextView)p2.findViewById(R.id.tvTitle);
            TextView tv2=(TextView)p2.findViewById(R.id.tvPhone);
            TextView tv3=(TextView)p2.findViewById(R.id.tvCity);
            tv1.setText(searchdata.get(p1).title);
            tv2.setText(searchdata.get(p1).snippet);
            tv3.setText(searchdata.get(p1).city);
            return p2;
        }
        public void filter(String query){
            query = query.toLowerCase();
            searchdata.clear();
            if(query.length() == 0){
               searchdata.addAll(data);
            }else {
                for (MarkersData md : data){
                    if(md.city.toLowerCase().contains(query)){
                        searchdata.add(md);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public void dialogBox(String name,String phone,double lat,double lon) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Do you want to call "+ name +"?");
        alertDialogBuilder.setPositiveButton("CALL",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (phone.trim().length() > 0) {
                            if (ContextCompat.checkSelfPermission(DataListActivity.this,
                                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(DataListActivity.this,
                                        new String[]{Manifest.permission.CALL_PHONE}, 1);
                            } else {
                                String dial = "tel:" + phone;
                                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                            }
                        } else {
                            Toast.makeText(DataListActivity.this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
                        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG);
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

}




