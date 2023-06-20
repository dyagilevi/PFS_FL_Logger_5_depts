package com.example.pfs_flowline_vermanufacture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Fragment  Frag_timer = new TimerFragment(), Frag_logs = new LogsFragment(), Frag_video = new VideoFragment();
    Animation rotateOpen,rotateClose, fromBottom, toBottom;
    FloatingActionButton fab_add, fab_logout;
    String Username, Station, Department;
    private BottomNavigationView bottomNavigationView;
    Fragment fragment;
    boolean clicked = false;
    FragmentManager fragmentManager = getSupportFragmentManager();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle bundle = getIntent().getExtras();
        Username = bundle.getString("EXTRA_USERNAME");
        Station = bundle.getString("EXTRA_STATION");
        Department = bundle.getString("EXTRA_DEPARTMENT");

        rotateOpen= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_open_anim);
        rotateClose= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_close_anim);
        fromBottom= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.from_bottom_anim);
        toBottom= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.to_bottom_anim);

        fab_add = (FloatingActionButton) findViewById(R.id.fab_add);
        fab_add.setOnClickListener(this);
        fab_logout = (FloatingActionButton) findViewById(R.id.fab_logout);
        fab_logout.setOnClickListener(this);

        if(null == savedInstanceState){
            initialiseFragments(Username, Station, Department);
        }
        bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(bottomNavMethod);

    }

    private BottomNavigationView.OnNavigationItemSelectedListener bottomNavMethod = new
            BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuitem) {
                    fragment = null;
                    switch (menuitem.getItemId()) {

                        case R.id.Logs:
                           // fragment = Frag_logs;
                           fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag("logs")).hide(fragmentManager.findFragmentByTag("timer")).hide(fragmentManager.findFragmentByTag("video")).commit();
                            break;

                        case R.id.Timer:
                           // fragment = Frag_timer;

                            fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag("timer")).hide(fragmentManager.findFragmentByTag("logs")).hide(fragmentManager.findFragmentByTag("video")).commit();
                            break;
                        case R.id.video:
                            //fragment = Frag_video;
                            fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag("video")).hide(fragmentManager.findFragmentByTag("timer")).hide(fragmentManager.findFragmentByTag("logs")).commit();
                            break;
                    }
                    //getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();

                    return true;
                }
            };

    private void initialiseFragments(String Username, String Station, String Department) {
       // Frag_timer = new TimerFragment();
       // Frag_logs = new LogsFragment();
       // Frag_video = new VideoFragment();
        Bundle bundleToFragment = new Bundle();
        bundleToFragment.putString("EXTRA_USERNAME",Username);
        bundleToFragment.putString("EXTRA_STATION",Station);
        bundleToFragment.putString("EXTRA_DEPARTMENT",Department);
        Frag_timer.setArguments(bundleToFragment);


        Bundle bundleToLogFragment = new Bundle();
        bundleToLogFragment.putString("EXTRA_USERNAME",Username);
        bundleToLogFragment.putString("EXTRA_STATION",Station);
        bundleToLogFragment.putString("EXTRA_DEPARTMENT",Department);
        Frag_logs.setArguments(bundleToLogFragment);
        fragmentManager.beginTransaction().add(R.id.container,Frag_timer, "timer")
                .add(R.id.container,Frag_logs,"logs")
                .add(R.id.container,Frag_video,"video")
                .show(Frag_timer)
                .hide(Frag_logs)
                .hide(Frag_video)
                .commit();

       /* getSupportFragmentManager().beginTransaction().add(R.id.container, Frag_timer)
                .add(R.id.container, Frag_id)
                .add(R.id.container, Frag_video)
                .add(R.id.container, Frag_logs)
                .hide(Frag_logs)
                .hide(Frag_id)
                .hide(Frag_video)
                .commit();
        */
    }

    //region FAB
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab_add:
               // Toast.makeText(this, "add button clicked", Toast.LENGTH_SHORT).show();
                onAddButtonClicked();
                break;

            case R.id.fab_logout:
                AlertLogout();
                //Toast.makeText(this, "logout button clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void AlertLogout()
    {
        AlertDialog.Builder logoutAlert = new AlertDialog.Builder(this);
        logoutAlert.setTitle("Logout");
        logoutAlert.setMessage("Are you sure you want to logout?");
        logoutAlert.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               finish();
                Toast.makeText(getApplicationContext(), "user Logged out", Toast.LENGTH_SHORT).show();
            }

        });

        logoutAlert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog alert = logoutAlert.create();
        alert.show();
    }

    private void onAddButtonClicked() {
        setVisibility();
        setAnimation();
        setClicked();
        clicked = !clicked;
    }

    private void setVisibility( ) {
        if(!clicked){
            fab_logout.setVisibility(View.VISIBLE);
        }else{
            fab_logout.setVisibility(View.INVISIBLE);
        }
    }

    private void setAnimation() {
        if(!clicked){
            fab_logout.startAnimation(fromBottom);
            fab_add.startAnimation(rotateOpen);
        }else{
            fab_logout.startAnimation(toBottom);
            fab_add.startAnimation(rotateClose);
        }
    }

    private  void setClicked(){
        if(!clicked){
            fab_logout.setClickable(true);
        }else{
            fab_logout.setClickable(false);
        }
    }

    //endregion FAB


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}