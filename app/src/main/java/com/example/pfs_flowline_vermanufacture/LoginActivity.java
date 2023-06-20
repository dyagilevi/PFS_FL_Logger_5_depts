package com.example.pfs_flowline_vermanufacture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.pfs_flowline_vermanufacture.BuildConfig.*;

//This is the login activity that will check the username and get station number
public class LoginActivity extends AppCompatActivity implements TextView.OnEditorActionListener, View.OnClickListener {

    //declare variables
    EditText et_userLogin;
    String userNameInput, station ,department;
    int departmentPosition, stationPosition;
    Boolean isUserValidation = false;
    AlertDialog progressDialog;

    ImageButton preferenceSettingButton;

    String googleScriptLogin;
    String googleScriptGetStations;

    Spinner sp_department, sp_station;
    TextView tv_sp_station;

    List<String> StationList;
    String[] StationArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Get version name
        String versionName = VERSION_NAME;

        // Get version code
        int versionCode = VERSION_CODE;

        TextView tv_version = (TextView) findViewById(R.id.tv_version);
        tv_version.setText("Version Name: " + versionName + "\n Version Code: " + versionCode);

        et_userLogin = (EditText) findViewById(R.id.et_userLogin);
        et_userLogin.setOnEditorActionListener(this);

        preferenceSettingButton = findViewById(R.id.preferenceSettingButton);
        preferenceSettingButton.setOnClickListener(this);

        // Retrieving the value using its keys the file name
        // must be same in both saving and retrieving the data
        @SuppressLint("WrongConstant") SharedPreferences sh = getSharedPreferences("timeLoggerPreference", MODE_APPEND);

        // The value will be default as empty string because for
        // the very first time when the app is opened, there is nothing to show
        department = sh.getString("department", "");
        station = sh.getString("station", "");

        departmentPosition = sh.getInt("departmentPosition", 0);
        stationPosition = sh.getInt("stationPosition", 0);

        googleScriptLogin =getGoogleSheetLoginURL(department);
        googleScriptGetStations= getGoogleSheetStationURL(department);
        getStations();
    }

    private String getGoogleSheetStationURL(String department){
        String url = "";

        switch (department){
            case "PFS":
                url = getResources().getString(R.string.googleScriptGetStationsPFS);
                break;

            case "Convection":
                url = getResources().getString(R.string.googleScriptGetStationsConvection);
                break;

            case "Inductor":
                url = getResources().getString(R.string.googleScriptGetStationsInductor);
                break;

            default:
                url = getResources().getString(R.string.googleScriptGetStationsPFS);
                break;

        }

        return url;
    }

    private String getGoogleSheetLoginURL(String department){
        String url = "";

        switch (department){
            case "PFS":
                url = getResources().getString(R.string.googleScriptLoginPFS);
                break;

            case "Convection":
                url = getResources().getString(R.string.googleScriptLoginConvection);
                break;

            case "Inductor":
                url = getResources().getString(R.string.googleScriptLoginInductor);
                break;

            default:
                url = getResources().getString(R.string.googleScriptLoginPFS);
                break;

        }

        return url;
    }

    //when user is done typing...
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event != null &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (event == null || !event.isShiftPressed()) {
                // the user is done typing.
                switch (v.getId()) {
                    case R.id.et_userLogin:
                        userlogin();
                }
            }
            return true; // consume.
        }
        return false;
    }

    //get userNameInput and validate user name
    private void userlogin() {
        userNameInput = et_userLogin.getText().toString();
        isUserValidation = false;
        ValidateUser();
    }

    //region validation
    private void ValidateUser() {

        //set loading dialog
        setProgressDialog("Logging in");
        progressDialog.show();

        //do a get method to get name registered
        StringRequest stringRequest = new StringRequest(Request.Method.GET, googleScriptLogin,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Once done and response is received parse the data and dismiss the dialog
                        parseItems(response);
                        progressDialog.dismiss();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        );

        int socketTimeOut = 50000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        stringRequest.setRetryPolicy(policy);

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    //parse item from google sheet data base (JSON)
    private void parseItems(String jsonResponse) {
        try {
            JSONObject jobj = new JSONObject(jsonResponse);
            JSONArray jarray = jobj.getJSONArray("items");

            // Get each name, see it if matches the user's input
            for (int i = 0; i < jarray.length(); i++) {
                JSONObject jo = jarray.getJSONObject(i);
                String userName = jo.getString("userName");

                //if username is valid, set userValidation to true
                if (userName.contentEquals(userNameInput)) {
                    isUserValidation = true;
                    break;
                } else {
                    //log tagging for debug purposes
                    Log.d("length", "string Database " + userName.length() + "string userInput" + userNameInput.length());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //if user is valid, open mainActivity, otherwise ask if the new user name wanted to be registered
        if (isUserValidation == true) {
            //et_User.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_baseline_done_24,0);
            et_userLogin.setText("");

            if(station != "" || department != ""){
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("EXTRA_USERNAME", userNameInput);
                intent.putExtra("EXTRA_STATION", station);
                intent.putExtra("EXTRA_DEPARTMENT", department.toString());
                startActivity(intent);
                Toast.makeText(this, "Welcome " + userNameInput, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Please Set Department and Station", Toast.LENGTH_SHORT).show();
            }

        } else {
            //et_User.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_error_outline_24, 0);;
            Toast.makeText(this, "user Name is not valid", Toast.LENGTH_SHORT).show();
            registerUser();
        }
    }
    //endregion

    //set cursor focus on edit text
    private void setFocus() {
        et_userLogin.requestFocus();
        InputMethodManager imm = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et_userLogin, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    //region Register user
    private void registerUser()
    {
        AlertDialog.Builder registerAlert = new AlertDialog.Builder(this);
        registerAlert.setTitle("Register new username");
        registerAlert.setMessage("User is not registered. Do your want to register a new username?");
        registerAlert.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addUserToSheet();
            }
        });

        registerAlert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setFocus();
                et_userLogin.setText("");
            }
        });

        AlertDialog alert = registerAlert.create();
        alert.show();
    }

    private void addUserToSheet(){
        setProgressDialog("Registering new user");
        progressDialog.show();
        final String User = et_userLogin.getText().toString().trim();

        // Do we need to check for duplicates first?
        StringRequest stringRequest = new StringRequest(Request.Method.POST, googleScriptLogin,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this,response,Toast.LENGTH_LONG).show();
                        et_userLogin.setText("");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parmas = new HashMap<>();
                //here vs pass params
                parmas.put("action","addUser");
                parmas.put("userName",User);
                return parmas;
            }
        };

        int socketTimeOut = 50000; // this is 50 seconds

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut,0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(this);

        queue.add(stringRequest);
    }

    //endregion

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void setProgressDialog(String message) {
        View v = getLayoutInflater().inflate(R.layout.progress_bar, null);
        TextView tvProgressBar = v.findViewById(R.id.tvProgressBar);
        tvProgressBar.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setView(v);

        progressDialog = builder.create();

        Window window = progressDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(progressDialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            progressDialog.getWindow().setAttributes(layoutParams);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.preferenceSettingButton:
                openPreferenceSettingPopUp(v);
                break;
        }
    }

    private void getStations() {
        setProgressDialog("Getting Stations");
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                StringRequest stringRequest = new StringRequest(Request.Method.GET, googleScriptGetStations,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                parseStations(response);
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });

                int socketTimeOut = 50000;
                RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut,0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                stringRequest.setRetryPolicy(retryPolicy);

                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                queue.add(stringRequest);
            }
        }).start();

    }


    private void parseStations(String jsonResponse) {

        StationList = new ArrayList<String>();

        try {
            JSONObject jobj = new JSONObject(jsonResponse);
            JSONArray jarray = jobj.getJSONArray("items");

            for (int i = 0; i < jarray.length(); i++) {

                JSONObject jo = jarray.getJSONObject(i);
                String id = jo.getString("id");
                String stations = jo.getString("stations");

                StationList.add(stations);

            }

            StationArray = new String[StationList.size()];
            StationArray = StationList.toArray(StationArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        progressDialog.dismiss();
    }

    private void updateAdapter(Context context ){
        setProgressDialog("Getting Stations");
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                StringRequest stringRequest = new StringRequest(Request.Method.GET, googleScriptGetStations,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                parseUpdater(response, context);
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });

                int socketTimeOut = 50000;
                RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut,0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                stringRequest.setRetryPolicy(retryPolicy);

                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                queue.add(stringRequest);
            }
        }).start();
    }

    private void parseUpdater(String Response, Context context){
        StationList = new ArrayList<String>();

        try {
            JSONObject jobj = new JSONObject(Response);
            JSONArray jarray = jobj.getJSONArray("items");

            for (int i = 0; i < jarray.length(); i++) {

                JSONObject jo = jarray.getJSONObject(i);
                String id = jo.getString("id");
                String stations = jo.getString("stations");

                StationList.add(stations);

            }

            StationArray = new String[StationList.size()];
            StationArray = StationList.toArray(StationArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> adapterStation = new ArrayAdapter<String>(context,R.layout.color_spinner_layout, StationArray);
        adapterStation.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        sp_station.setAdapter(adapterStation);

        progressDialog.dismiss();
    }


    private void openPreferenceSettingPopUp(View view){
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.setting_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        sp_department = popupWindow.getContentView().findViewById(R.id.sp_department);
        sp_station = popupWindow.getContentView().findViewById(R.id.sp_station);
        tv_sp_station = popupWindow.getContentView().findViewById(R.id.tv_sp_station);

        // populate spinner department
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(popupView.getContext(), R.array.departments, R.layout.color_spinner_layout);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        sp_department.setAdapter(adapter);

        // set default value or last saved value
        sp_department.setSelection(departmentPosition);

        // populate spinner station
        ArrayAdapter<String> adapterStation = new ArrayAdapter<String>(popupView.getContext(),R.layout.color_spinner_layout, StationArray);
        adapterStation.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        sp_station.setAdapter(adapterStation);

        //set default value or last value
        sp_station.setSelection(stationPosition);

        sp_department.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                //get station input and display toast
                department = parentView.getItemAtPosition(position).toString();
                if(position != departmentPosition) {
                    departmentPosition = position;
                    googleScriptLogin = getGoogleSheetLoginURL(department);
                    googleScriptGetStations = getGoogleSheetStationURL(department);

                    Toast.makeText(parentView.getContext(), department + " department selected, please select the station ", Toast.LENGTH_SHORT).show();
                    updateAdapter(popupView.getContext());

                }
                else {
                    ArrayAdapter<String> adapterStation = new ArrayAdapter<String>(popupView.getContext(),R.layout.color_spinner_layout, StationArray);
                    adapterStation.setDropDownViewResource(R.layout.spinner_dropdown_layout);
                    sp_station.setAdapter(adapterStation);
                    sp_station.setSelection(stationPosition);
                }
                sp_station.setVisibility(View.VISIBLE);
                tv_sp_station.setVisibility(View.VISIBLE);

                sp_station.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        station = parent.getItemAtPosition(position).toString();
                        stationPosition = position;

                        Toast.makeText(parentView.getContext(), station + " station selected", Toast.LENGTH_SHORT).show();

                        // Storing data into SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("timeLoggerPreference",MODE_PRIVATE);

                        // Creating an Editor object to edit(write to the file)
                        SharedPreferences.Editor myEdit = sharedPreferences.edit();

                        // Storing the key and its value as the data fetched from edittext
                        myEdit.putString("department", department);
                        myEdit.putInt("departmentPosition", departmentPosition);
                        myEdit.putString("station", station);
                        myEdit.putInt("stationPosition", position);

                        // Once the changes have been made,
                        // we need to commit to apply those changes made,
                        // otherwise, it will throw an error
                        myEdit.apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                Toast.makeText(parentView.getContext(), "Nothing is selected!" , Toast.LENGTH_SHORT).show();
            }

        });

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }
}