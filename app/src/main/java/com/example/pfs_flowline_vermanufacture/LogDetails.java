package com.example.pfs_flowline_vermanufacture;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class LogDetails extends AppCompatActivity {

    TextView ChargerCode, ModuleNum, Date, Comments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_details);
        Intent intent = getIntent();


        ChargerCode = findViewById(R.id.tv_details_chargerCode);
        Comments = findViewById(R.id.tv_details_comments);
        Date = findViewById(R.id.tv_details_date);



        String chargerCodeText = intent.getStringExtra("chargerCode");
        String commentText = intent.getStringExtra("comments");
        String endText = intent.getStringExtra("end");

        ChargerCode.setText("Charger Code:" + chargerCodeText);
        Comments.setText(commentText);
        Date.setText("Date:" + endText);

    }
}