package com.example.TATP_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

public class SubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        Button bkbutton = findViewById(R.id.b);
        bkbutton.setOnClickListener(v ->
                finish());
    }
}