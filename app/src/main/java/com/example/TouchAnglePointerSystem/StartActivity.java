package com.example.TouchAnglePointerSystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class StartActivity extends AppCompatActivity {

    public Boolean practiceflg = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        TextView text3 = findViewById(R.id.textView3);
        text3.setText("練習or実験本番");
        Button prabtn = findViewById(R.id.practice);
        prabtn.setOnClickListener(v -> {
            practiceflg = true;
            text3.setText("練習");
        });
        Button probtn = findViewById(R.id.production);
        probtn.setOnClickListener(v -> {
            practiceflg = false;
            text3.setText("実験本番");
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