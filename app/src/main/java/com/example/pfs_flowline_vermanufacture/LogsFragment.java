package com.example.pfs_flowline_vermanufacture;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

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
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LogsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogsFragment extends Fragment implements RecyclerAdapter.OnItemListener {

    public LogsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChartFragment.
     */

    public static LogsFragment newInstance(String param1, String param2) {
        LogsFragment fragment = new LogsFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String mParam1 = getArguments().getString("param1");
            String mParam2 = getArguments().getString("param2");
        }
    }

    RecyclerView recyclerView;
    AlertDialog progressDialog;
    String userNameInput, stationNumInput, departmentInput;
    private    ArrayList<HashMap<String, String>> list, listSerial;

    String googleScriptUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_chart, container, false);
        Bundle getBundle = getArguments();
        userNameInput = getBundle.getString("EXTRA_USERNAME");
        stationNumInput = getBundle.getString("EXTRA_STATION");
        departmentInput = getBundle.getString("EXTRA_DEPARTMENT");

        googleScriptUrl = getGoogleSheetURL(departmentInput);

        recyclerView = (RecyclerView) v.findViewById(R.id.rv_items);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);

        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getItems();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        getItems();
        return v;
    }

    private String getGoogleSheetURL(String department){

        int url = 0;

        switch (department){
            case "PFS":
                url = R.string.googleScriptUrlGetLogsPFS;
                break;

            case "Convection":
                url = R.string.googleScriptUrlGetLogsConvection;
                break;

            case "Inductor":
                url = R.string.googleScriptUrlGetLogsInductor;
                break;

        }

        return getResources().getString(url);
    }

    private void getItems() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                StringRequest stringRequest = new StringRequest(Request.Method.GET, googleScriptUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                parseItems(response);
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });

                int socketTimeOut = 50000;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

                stringRequest.setRetryPolicy(policy);

                RequestQueue queue = Volley.newRequestQueue(getActivity());
                queue.add(stringRequest);
            }
        }).start();

    }


    private void parseItems(String jsonResposnce) {

        list = new ArrayList<>();
        listSerial = new ArrayList<>();

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
                String comments = jo.getString("comments");

                if(station.compareTo(stationNumInput) == 0 && userName.compareTo(userNameInput) == 0){
                    HashMap<String, String> item = new HashMap<>();
                    item.put("id", id);
                    item.put("station", station);
                    item.put("userName", userName);
                    item.put("chargerCode", chargerCode);
                    item.put("start", start);
                    item.put("end", end);
                    item.put("duration",duration);
                    item.put("status",status);
                    item.put("conformation", conformation);
                    item.put("conformationType", conformationType);
                    item.put("comments", comments);

                    list.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Collections.sort(list, new MapComparator("end"));
        RecyclerAdapter Adapter1 = new RecyclerAdapter(getActivity(),list,this);
        recyclerView.setAdapter(Adapter1);
        //progressDialog.dismiss();
    }

    //set on item click listener
    @Override
    public void onItemClick(int position) {

        if(stationNumInput.compareTo("Quality Assessor") == 0){
            //get data from list using the position of item
            HashMap<String,String> map = list.get(position);
            Intent intent = new Intent (getActivity(), LogDetails.class);

            //String id = map.get("id").toString();
            String chargerCode = map.get("chargerCode").toString();
            String comments = map.get("comments");
            String end = map.get("end");



            //put data to intent
            //intent.putExtra("id",id);
            intent.putExtra("chargerCode",chargerCode);
            intent.putExtra("comments",comments);
            intent.putExtra("end",end);



            startActivity(intent);
        }

    }


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


                HashMap<String, String> item = new HashMap<>();
                item.put("Barcode", Barcode);
                item.put("ModuleNumber", ModuleNumber);
                item.put("Cells", Cells);
                item.put("PowerRating", PowerRating);
                item.put("Phase", Phase);
                item.put("Voltage", Voltage);
                item.put("VariantName",VariantName);
                item.put("Profile",Profile);

                listSerial.add(item);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


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
}