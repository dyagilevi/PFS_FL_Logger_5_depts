package com.example.pfs_flowline_vermanufacture;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static androidx.core.content.ContextCompat.getSystemService;

/**
 * This is a fragment for timer and submitting log to google sheet
 */
public class TimerFragment<ScheduledExecutorService> extends Fragment implements TextView.OnEditorActionListener, View.OnClickListener {

    public TimerFragment() {
        // Required empty public constructor
    }

    public static TimerFragment newInstance(String param1, String param2) {
        TimerFragment fragment = new TimerFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    //region variables
    TextView tv_startTime, tv_endTime, tv_duration, tv_user,tv_module,tv_station,tv_stationLogCount;
    EditText et_barcode,et_note;
    ImageView iv_status, iv_stop;
    ConstraintLayout relativeLayout2;

    AlertDialog progressDialog;
    AlertDialog.Builder helpAlert;

    Boolean isUserValidation = false, isStationExist;
    String userNameInput, stationNumInput, moduleInput,startInput,EndInput,durationInput,status, conformation, conformationType, comments="", departmentInput;
    String temporary;
    Timer timer;
    TimerTask timerTask;
    private Handler focusHandler, logCountHandler;
    double time = 0.0;
    java.util.concurrent.ScheduledExecutorService focus = Executors.newScheduledThreadPool(1);
    int logCount;

    Animation animBlink;

    private NotificationManagerCompat notificationManager;

    int googleScriptUrlPost;
    int googleScriptUrlGetLogs;


    private enum TimerState {
        STOPPED,
        RUNNING,
        PAUSED
    }

    private TimerState timerState = TimerState.STOPPED;

    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_timer, container, false);

        //declare variables
        //tv_startTime = (TextView) v.findViewById(R.id.tv_startTime);
        //tv_endTime = (TextView) v.findViewById(R.id.tv_endTime);
        et_note = v.findViewById(R.id.et_note);
        relativeLayout2 = v.findViewById(R.id.relativeLayout2);
        tv_duration = v.findViewById(R.id.tv_duration);
        tv_station = v.findViewById(R.id.tv_station);
        tv_user = v.findViewById(R.id.tv_user);
        tv_module = v.findViewById(R.id.tv_module);
        tv_stationLogCount = v.findViewById(R.id.tv_stationLogCount);

        //get username and station from activity
        Bundle getBundle = getArguments();
        userNameInput = getBundle.getString("EXTRA_USERNAME");
        stationNumInput = getBundle.getString("EXTRA_STATION");
        departmentInput = getBundle.getString("EXTRA_DEPARTMENT");

        googleScriptUrlPost = getGoogleSheetURL(departmentInput)[0];
        googleScriptUrlGetLogs = getGoogleSheetURL(departmentInput)[1];

        iv_status = (ImageView) v.findViewById(R.id.iv_status);


        iv_stop = (ImageView) v.findViewById(R.id.iv_stop);

        focusHandler = new Handler();

        //set preset value of status and conformation
        if(stationNumInput.compareTo("Quality Assessor") == 0 || stationNumInput.compareTo("FA3") == 0
        || stationNumInput.compareTo("PnP") == 0 || stationNumInput.compareTo("Coring") == 0
                || stationNumInput.compareTo("Winding OPCH") == 0){
            et_note.setVisibility(View.VISIBLE);
            status = "complete";
        }else {
            status = "in progress";

            //set focus handler if not QA
            focusHandler.post(focusRunnable);
        }

        conformation = "N/A";
        conformationType = "N/A";
        comments="";

        if(stationNumInput.compareTo("Wire Cutting") == 0 || stationNumInput.compareTo("Fine Soldering") == 0
                ||stationNumInput.compareTo("OPL/Toroid") == 0 || stationNumInput.compareTo("Kitting") == 0 ){
            status = "Sub Assemblies";
            iv_status.setOnClickListener(this);
        }

        iv_stop.setOnClickListener(this);

        //set textView preset
        tv_user.setText(userNameInput);
        tv_station.setText( departmentInput + " - " + stationNumInput);

        et_barcode = (EditText) v.findViewById(R.id.et_barcode);
        et_barcode.setOnEditorActionListener(this);
        et_barcode.setHint("Module Barcode");
        et_barcode.requestFocus();

        // load the animation
        animBlink = AnimationUtils.loadAnimation(getActivity(),
                R.anim.blink);

        //declare timer
        timer = new Timer();

        //initialise handler
        logCountHandler = new Handler();


        //run handler
        logCountHandler.post(logCountRunnable);


        notificationManager = NotificationManagerCompat.from(getActivity());

        createNotificationChannels();


        return v;
    }

    private int[] getGoogleSheetURL(String department){

        int[] url = new int[2];

        switch (department){
            case "PFS":
                url[0] = R.string.googleScriptUrlPostPFS;
                url[1] = R.string.googleScriptUrlGetLogsPFS;
                break;

            case "Convection":
                url[0] = R.string.googleScriptUrlPostConvection;
                url[1]= R.string.googleScriptUrlGetLogsConvection;
                break;

            case "Inductor":
                url[0] = R.string.googleScriptUrlPostInductor;
                url[1]= R.string.googleScriptUrlGetLogsInductor;
                break;

        }

        return url;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.iv_status:
                if(timerState == TimerState.STOPPED){

                    //set flashing off
                    relativeLayout2.setBackgroundColor(Color.BLACK);
                    relativeLayout2.clearAnimation();
                    iv_stop.setVisibility(View.VISIBLE);

                    //start timer
                    startTimer();

                    // Set to TimerRunning state
                    timerState = TimerState.RUNNING;

                    //change play image into pause image
                    iv_status.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);

                    //send toast "timer started"
                    Toast.makeText(getActivity(), "Timer Started", Toast.LENGTH_SHORT).show();

                    //get start time
                    startInput = getNow();

                    //get time started and display to textView
                    //tv_startTime.setText(startInput);

                    //get moduleInput
                    moduleInput = "N/A";
                    tv_module.setText(moduleInput);
                    iv_stop.setVisibility(View.VISIBLE);

                }else if (timerState != TimerState.STOPPED) {
                    StartpauseTimer();}
                break;

            case R.id.iv_stop:
                if ( timerState != TimerState.STOPPED) {

                    if (stationNumInput.contentEquals("Quality Assessor")) {
                        ConformationDialog();
                    }
                    else {
                        StopTimer(Boolean.FALSE);
                    }

                    iv_stop.setVisibility(View.GONE);
                    iv_status.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
                }
                break;
        }


    }

    //check edit Text focus every 20 seconds
    private Runnable focusRunnable = new Runnable() {
        @Override
        public void run() {

            //if editText barcode has no focus
            if(!et_barcode.hasFocus())
            {
                //focus editText
                focusBarcodeText();
            }
            //re-run handler with 20 second delay
            focusHandler.postDelayed(focusRunnable, 20000);
        }
    };


    //get log count every 5 minutes
    private Runnable logCountRunnable = new Runnable() {
        @Override
        public void run() {
            //get data from google sheet
            getItems();
            if(timerState.equals(TimerState.STOPPED)){
                //send reminder notification
                notification();
                relativeLayout2.setBackgroundColor(Color.RED);
                relativeLayout2.startAnimation(animBlink);
            }
            //re-run handler with 5 minute (300000 millis) delay
            logCountHandler.postDelayed(logCountRunnable, 300000);

        }
    };


    public static final String CHANNEL_LOGGER_REMINDER = "Reminder";

    private void createNotificationChannels(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel reminder = new NotificationChannel(
                    CHANNEL_LOGGER_REMINDER,
                    "Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                    );
            reminder.setDescription("This is the reminder channel");
            NotificationManager manager = getSystemService(getActivity(),NotificationManager.class);
            manager.createNotificationChannel(reminder);
        }
    }

    private void notification(){
        Notification notification = new NotificationCompat.Builder(this.requireContext(),CHANNEL_LOGGER_REMINDER)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Reminder")
                .setContentText("Please ensure you log your work!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1,notification);
    }


    // Action in barcode text.
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
            actionId == EditorInfo.IME_ACTION_DONE ||
            event != null &&
            event.getAction() == KeyEvent.ACTION_DOWN &&
            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

            if (event == null || !event.isShiftPressed()) {
                ProcessAction();
                return true;
            }
        }
        return false;
    }

    // Evaluate the action in the barcode text
    private void ProcessAction() {

        String barcodeText = et_barcode.getText().toString();

        //check if the barcode starts with SE and NotStarted
        if(barcodeText.startsWith("SE") && timerState == TimerState.STOPPED) { Start(barcodeText); }

        // Help available in any state
        else if (barcodeText.contentEquals("Help")) { HelpDialog(); }

        else if (barcodeText.contentEquals("StartPause") && timerState != TimerState.STOPPED) { StartpauseTimer(); }

        else if (barcodeText.contentEquals("Stop") && timerState != TimerState.STOPPED ) {

            if (stationNumInput.contentEquals("Quality Assessor")) { ConformationDialog(); }
            else { StopTimer(Boolean.FALSE); }
        }

        else if(barcodeText.startsWith("SE") && timerState != TimerState.STOPPED){

            if (stationNumInput.contentEquals("Quality Assessor")) {ConformationDialog(); }
            else {StopTimer(Boolean.TRUE); }

            //make stop button disappear
            iv_stop.setVisibility(View.GONE);

        }

        // Barcode invalid: send toast, clear text, and refocus
        else {Toast.makeText(getActivity(),"Invalid Barcode",Toast.LENGTH_SHORT).show();}

        temporary = barcodeText;
        et_barcode.setText("");

        // Reset focus on the barcode text
        focusBarcodeText();
    }

    private void Start(String barcodeText){
        relativeLayout2.setBackgroundColor(Color.BLACK);
        relativeLayout2.clearAnimation();
        iv_stop.setVisibility(View.VISIBLE);
        //start timer
        startTimer();

        // Set to TimerRunning state
        timerState = TimerState.RUNNING;

        //change play image into pause image
        iv_status.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);

        //send toast "timer started"
        Toast.makeText(getActivity(), "Timer Started", Toast.LENGTH_SHORT).show();

        //get start time
        startInput = getNow();

        //get time started and display to textView
        //tv_startTime.setText(startInput);

        if (departmentInput.equals("Inductor")){
            tv_module.setText(barcodeText);
        }
        else{
            modelNums(barcodeText);
        }

        //Set hint
        et_barcode.setHint("Command Line");
    }

    // Stop timer and Log time
    private void StopTimer(Boolean restart) {
        timerTask.cancel();
        durationInput = tv_duration.getText().toString();
        time = 0.0;
        timerState = TimerState.STOPPED;
        EndInput = getNow();
        //tv_endTime.setText(EndInput);
        iv_stop.setVisibility(View.GONE);

        comments = et_note.getText().toString();
        moduleInput = tv_module.getText().toString();
        addLogToSheet(restart,userNameInput,stationNumInput,moduleInput,startInput,EndInput,durationInput,status,conformation,conformationType,comments);
    }


    public void focusBarcodeText()
    {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            et_barcode.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
            imm.showSoftInput(et_barcode, InputMethodManager.SHOW_IMPLICIT);
        }, 1000);
    }

    //----------------------------------------------------------------------------------------------
    //region Conforming dialogs

    // Dialog for evaluating whether a charger conforms with the standards
    private void ConformationDialog() {
        AlertDialog.Builder faultAlert = new AlertDialog.Builder(getActivity());
        faultAlert.setTitle("Quality Assessor");
        faultAlert.setMessage("Is the module conforming to the standards?");
        faultAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                conformation = "conforming";
                StopTimer(Boolean.FALSE);
            }
        });

        // If non conforming, then retreive conformation type/classification
        faultAlert.setNeutralButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                conformation = "non-conforming";
                NonConformationDialog();
            }
        });

        AlertDialog alert = faultAlert.create();
        alert.show();
    }

    // Dialog for non-conforming chargers (station Quality Assessor)
    private void NonConformationDialog(){
        AlertDialog.Builder conformationTypeDialog = new AlertDialog.Builder(getActivity());
        conformationTypeDialog.setTitle("Please select the issue(s)");
        boolean[] checkedItems = new boolean[]{false, false, false, false, false};

        // Fault type options
        String[] faultType = getResources().getStringArray(R.array.faultTypes);

        // Resizable array for multi-choice selection
        ArrayList<String> SelectedFaultType = new ArrayList<String>();

        conformationTypeDialog.setMultiChoiceItems(faultType, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                if(isChecked == true){
                    SelectedFaultType.add(faultType[which]);
                } else {

                    SelectedFaultType.remove(faultType[which]);
                }
            }
        });

        conformationTypeDialog.setPositiveButton("Feedback submitted", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                conformationType = TextUtils.join(",", SelectedFaultType);
                //CommentsDialog();
                StopTimer(Boolean.FALSE);
                SelectedFaultType.clear();
            }
        });

        AlertDialog alert = conformationTypeDialog.create();
        alert.show();
    }

    // endregion
    //----------------------------------------------------------------------------------------------
    //region Help requests

    // Help dialog - can be called at any stage
    private void HelpDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.alert_help, null);;
        helpAlert = new AlertDialog.Builder(getContext());
        helpAlert.setView(dialogView);

        final EditText et_help = dialogView.findViewById(R.id.et_help);
        final Button bt_help_cancel = dialogView.findViewById(R.id.bt_help_cancel);
        final Button bt_help_submit= dialogView.findViewById(R.id.bt_help_submit);

        AlertDialog alert = helpAlert.create();

        bt_help_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.cancel();
            }
        });

        bt_help_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = et_help.getText().toString();
                sendHelpLog(message);
                sendGoogleChatText(message);
                alert.cancel();
            }
        });

        alert.show();
    }

    // Request help via Slack
    private void sendGoogleChatText(String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, Object> params = new HashMap<>();
                String message = String.format("*User:* %s \n*Department:* %s \n*Station:* %s \n*Requires assistance for:*\n%s",
                        userNameInput, departmentInput, stationNumInput, text);

                params.put("text", message);

                String url = getResources().getString(R.string.googleChatURL);

                StringRequest request = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            public void onResponse(String response) {
                                //progressDialog.dismiss();
                                Toast.makeText( getActivity(),response,Toast.LENGTH_LONG).show();
                                Log.d("DEBUG", "Google Chat Response - " + response);
                            }
                        },
                        new Response.ErrorListener() {
                            public void onErrorResponse(VolleyError error) {
                                Log.d("DEBUG", "Google Chat Error Response - *%s", error);
                            }
                        }
                ) {
                    public byte[] getBody() {
                        return new JSONObject(params).toString().getBytes();
                    }
                    public String getBodyContentType() {
                        return "application/json";
                    }
                };

                Volley.newRequestQueue(getActivity()).add(request);
            }
        }).start();

    }

    private void sendHelpLog(String reason) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String googleScriptUrl = getResources().getString(googleScriptUrlPost);

                StringRequest stringRequest = new StringRequest(Request.Method.POST, googleScriptUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                //progressDialog.dismiss();
                                Toast.makeText(getActivity(), response, Toast.LENGTH_LONG).show();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        })
                {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> parmas = new HashMap<>();
                        //here vs pass params
                        parmas.put("action", "addHelpLog");

                        parmas.put("station", stationNumInput);
                        parmas.put("userName", userNameInput);
                        parmas.put("reason", reason);

                        return parmas;
                    }

                };

                int socketTimeOut = 50000; // this is 50 seconds

                RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                stringRequest.setRetryPolicy(retryPolicy);

                RequestQueue queue = Volley.newRequestQueue(getActivity());

                queue.add(stringRequest);
            }
        }).start();
    }

    //endregion
    //----------------------------------------------------------------------------------------------

    //reset all variables/parameters
    private void resetAll() {
        tv_duration.setText("00:00:00");
        //tv_endTime.setText("N/A");
       tv_module.setText("Module");
        //tv_startTime.setText("N/A");
        moduleInput = "";
        startInput = "";
        EndInput = "";
        durationInput = "";
        conformation = "N/A";
        conformationType = "N/A";
        comments = "";

        //set all boolean to false
        isUserValidation = false;
        isStationExist= false;

        //set play image into Pause symbol
        iv_status.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);

        et_barcode.setHint("Module Barcode");
        et_note.setText("");
    }


    //send log to google sheet
    private void addLogToSheet(Boolean restart, String userName, String station, String module, String start, String end,String duration, String status, String conformation, String conformationType, String comments){
        userName.trim();
        station.trim();
        module.trim();
        start.trim();
        end.trim();
        duration.trim();
        status.trim();
        conformation.trim();
        conformationType.trim();
        comments.trim();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String googleScriptUrl = getResources().getString(googleScriptUrlPost);
                StringRequest stringRequest = new StringRequest(Request.Method.POST, googleScriptUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                //progressDialog.dismiss();
                                Toast.makeText(getActivity(),response,Toast.LENGTH_LONG).show();
                                resetAll();

                                if(restart){
                                    Start(temporary);
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(getActivity(),"Error while trying to post time: " + error,Toast.LENGTH_LONG).show();
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> parmas = new HashMap<>();
                        //here vs pass params
                        parmas.put("action","addLog");

                        parmas.put("station",station);
                        parmas.put("userName",userName);
                        parmas.put("chargerCode",module);
                        parmas.put("start",start);
                        parmas.put("end",end);
                        parmas.put("duration",duration);
                        parmas.put("status", status);
                        parmas.put("conformation", conformation);
                        parmas.put("conformationType", conformationType);
                        parmas.put("comments", comments);
                        return parmas;
                    }
                };

                int socketTimeOut = 50000; // this is 50 seconds

                RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut,0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                stringRequest.setRetryPolicy(retryPolicy);

                RequestQueue queue = Volley.newRequestQueue(getActivity());

                queue.add(stringRequest);
            }
        }).start();

    }

    //region Timer
    private String getNow() {
        LocalDateTime localDateTime = LocalDateTime.now();
        String todayformattedDate = localDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"));

        return todayformattedDate;
    }

    private void startTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        time++;
                        tv_duration.setText(getTimerText());
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0,1000);
    }

    private String getTimerText(){
        int rounded = (int) Math.round(time);

        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void StartpauseTimer() {
        if (timerState == TimerState.RUNNING) {
            timerTask.cancel();
            timerState = TimerState.PAUSED;

            iv_status.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
            Toast.makeText(getActivity(),"Timer paused", Toast.LENGTH_SHORT).show();
        } else {
            timerState = TimerState.RUNNING;
            startTimer();

            iv_status.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
            Toast.makeText(getActivity(),"Timer resumed", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion


    // After Stop - could this be a static method?
    public void setProgressDialog(String message) {
        View v = getLayoutInflater().inflate(R.layout.progress_bar, null);
        TextView tvProgressBar = v.findViewById(R.id.tvProgressBar);
        tvProgressBar.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    //region get Production Count from database
    private void getItems() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String googleScriptUrl =  getResources().getString(googleScriptUrlGetLogs);

                StringRequest stringRequest = new StringRequest(Request.Method.GET, googleScriptUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                parseItems(response);
                                //progressDialog.dismiss();
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

                RequestQueue queue = Volley.newRequestQueue(getActivity());
                queue.add(stringRequest);
            }
        }).start();
        //progressDialog.show();

    }

    private void parseItems(String jsonResposnce) {

        //get today's date
        LocalDate today = LocalDate.now();
        String formattedToday = today.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        logCount = 0;

        try {
            JSONObject jobj = new JSONObject(jsonResposnce);
            JSONArray jarray = jobj.getJSONArray("items");


            for (int i = 0; i < jarray.length(); i++) {

                JSONObject jo = jarray.getJSONObject(i);
                String id = jo.getString("id");
                String station = jo.getString("station");
                String userName = jo.getString("userName");
                String chargerCode = jo.getString("chargerCode");
                String start = jo.getString("start");
                String end = jo.getString("end");
                String duration = jo.getString("duration");
                String status = jo.getString("status");
                String conformation = jo.getString("conformation");
                String conformationType = jo.getString("conformationType");

                if( station.compareTo(stationNumInput) == 0 && dataDateFormatter(end).compareTo(formattedToday) == 0 && userName.compareTo(userNameInput) == 0){
                    logCount++;
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        tv_stationLogCount.setText(Integer.toString(logCount));
    }

    //Custom Date formatter function
    private String dataDateFormatter (String inputDate) {
        //original or typical string date for google sheet
        SimpleDateFormat formatDateTime = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
        //change to date only
        SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yy");

        //initialise date variable
        Date date =  new Date();
        try {
            //parse inputdate
            date = formatDateTime.parse(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //get formatted date and returns it
        String formattedDate = formatDate.format(date);

        return formattedDate;
    }

    static String[] TypeFromLabel =
            {
                    "PEI",
                    "UPEI",
                    "PMV",
                    "UPMV",
                    "PLV",
                    "UPLV",
                    "PFC",
                    "UPFC",
                    "PFS",
                    "UPFS",
                    "APX",
                    "UAPX",
                    "APXFC",
                    "UAPXFC"
            };

    static String[] JsonFileMapping =
            {
                    "CA", //Convection Type - A
                    "CA",
                    "CA",
                    "CA",
                    "CA",
                    "CA",
                    "FA", // FanCool - Double Board
                    "FA",
                    "FB", // FanCool - Single Board
                    "FB",
                    "CX", //Convection Type - APEX
                    "CX",
                    "FX", //Fan Cool - APEX
                    "FX"
            };

    private void modelNums(String serialNumber) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String portalUrl = getResources().getString(R.string.portalStanbury) + serialNumber;

                StringRequest stringRequest = new StringRequest(Request.Method.GET, portalUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                parseModel(response);
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        }){
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("Authorization", "Token 4551becf6cffe585b4dc4331bb789e3d62f71a78");
                        return params;
                    };
                };

                int socketTimeOut = 50000;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                stringRequest.setRetryPolicy(policy);

                RequestQueue queue = Volley.newRequestQueue(getActivity());
                queue.add(stringRequest);
            }
        }).start();

    }


    private void parseModel(String jsonResposnce) {
        String[] data = new String[8];
        Boolean isMV= Boolean.FALSE, isMC= Boolean.FALSE;

        try {
//            JSONObject jobj = new JSONObject(jsonResposnce);
//            JSONArray jarray = jobj.getJSONArray("");

            JSONArray jarray = new JSONArray("["+ jsonResposnce +"]");
            JSONObject jobj = jarray.getJSONObject(0);

            String Barcode = jobj.getString("Barcode");
            String ModuleNumber = jobj.getString("ModuleNumber");
            String Cells = jobj.getString("Cells");
            String PowerRating = jobj.getString("PowerRating");
            String Phase = jobj.getString("Phase");
            String Voltage = jobj.getString("Voltage");
            String VariantName = jobj.getString("VariantName");
            String Profile = jobj.getString("Profile");

            if(VariantName == "MC"){isMC = true;}
            if(Voltage.contains("/")){isMV = true;}

            String voltageRemovedV = Voltage.replace("V", "");

            data[0] = Barcode;
            data[1] = ModuleNumber;
            data[2] = Cells;
            data[3] = PowerRating;
            data[4] = Phase;
            data[5] = voltageRemovedV;
            data[6] = VariantName;
            data[7] = Profile;

            if(Profile.equals("PLV")){data[5] = "208";}



            //get moduleInput
            tv_module.setText(computedModelName(isMC, isMV, data));


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static String computedModelName (boolean IsMC, boolean IsMV, String[] data)
    {
        String phiSign = "Ø";
        String profile = "";

        for(int i = 0; i< TypeFromLabel.length; i++){
            if(data[7].equals(TypeFromLabel[i])){profile = JsonFileMapping[i];}
        }

        if (IsMC)
        {
            return String.format("%s-12/%s/%s%s%s", data[2],data[3], profile, IsMV ? "MV" : data[5], Integer.parseInt(data[4]) == 3 ? phiSign : "");
        }
        else
        {
            return String.format("%s/%s/%s%s%s", data[2],data[3],profile, IsMV ? "MV" : data[5], Integer.parseInt(data[4]) == 3 ? phiSign : "");
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        logCountHandler.removeCallbacks(logCountRunnable);
        focusHandler.removeCallbacks(focusRunnable);
    }
}