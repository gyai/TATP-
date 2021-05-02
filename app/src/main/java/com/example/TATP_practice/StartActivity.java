package com.example.TATP_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class StartActivity extends AppCompatActivity {

    public Boolean practiceflg = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Button prabtn = findViewById(R.id.practice);
        prabtn.setOnClickListener(v -> {
            practiceflg = true;

        });
        Button probtn = findViewById(R.id.production);
        probtn.setOnClickListener(v -> {
            practiceflg = false;

        });
        Button startbtn = findViewById(R.id.startbutton);
        startbtn.setOnClickListener(v -> {
            //インテントの作成
            Intent intent = new Intent(this, MainActivity.class);
            //データセット
            /*
            必要な初期情報
            ・練習か本番か（BOOL型）
            ・被験者番号-セクション番号
             */
            //被験者情報
            EditText editText =(EditText) this.findViewById(R.id.edit);
            intent.putExtra("statusText",editText.getText().toString());
            //本番フラグ
            intent.putExtra("practice",practiceflg);
            //遷移先の画面指定
            startActivity(intent);

        });
    }

}