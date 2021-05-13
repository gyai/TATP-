package com.example.TATP_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class SubActivity extends AppCompatActivity {
    public TextView textView = findViewById(R.id.textView);//DBとzip圧縮できたら完了コメント出す
    public String statustext;
    public List<String> taskcountlist = new ArrayList<>();
    public List<String> targetpointlist = new ArrayList<>();
    public List<String> sousatimelist = new ArrayList<>();
    public List<String> errorlist = new ArrayList<>();
    public String errorpercent;
    public List<String> trajectorylist = new ArrayList<>();
    public List<String> firsttouchpointlist = new ArrayList<>();
    public List<String> firstyo_list = new ArrayList<>();
    public List<String> firsthiritulist = new ArrayList<>();
    public List<String> imagenamearray = new ArrayList<>();
    public List<byte[]> imagearray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        Intent intent = this.getIntent();
        //被験者情報、(1セクション＝全体通して更新されない)
        statustext = intent.getStringExtra("statusText");
        // タスク番号
        taskcountlist = intent.getStringArrayListExtra("taskCount");
        // ターゲット座標(x,y)
        targetpointlist = intent.getStringArrayListExtra("targetPoint");
        // 操作時間
        sousatimelist = intent.getStringArrayListExtra("sousaTime");
        // エラー
        errorlist = intent.getStringArrayListExtra("errorString");
        //エラー回数/35=1セクションのエラー率
        errorpercent = intent.getStringExtra("errorCount");
        // ポインター軌跡
        trajectorylist = intent.getStringArrayListExtra("trajectory");
        // 初期指座標
        firsttouchpointlist = intent.getStringArrayListExtra("firstTouchPoint");
        // 初期指ヨー角
        firstyo_list = intent.getStringArrayListExtra("firstYo");
        // 初期指の長軸短軸（３５個分）。比率＝長軸長さ/短軸長さ。
        firsthiritulist = intent.getStringArrayListExtra("firstHiritu");
        // 画像名前（全画像分）
        imagenamearray = intent.getStringArrayListExtra("imageName");

        // 画像byte配列（全画像分）
        MainActivity mainActivity = new MainActivity();
        imagearray = mainActivity.sectionimagearray;

        //DB操作


        ///35タスクが終了し画面が遷移したら、画像フォルダを圧縮し、データを削除//
        compress(imagearray, statustext);


        Button bkbutton = findViewById(R.id.b);
        bkbutton.setOnClickListener(v -> {
            Intent restartintent = new Intent(getApplication(), StartActivity.class);
            startActivity(restartintent);
                }
        );
    }

    /**
     * @param List   inputFiles : 圧縮したいJPEGファイルのリストー＞byte配列で行けるのか微妙？？
     * @param String outputFile : 出力先となるZIPファイルのファイル名
     */
    public void compress(List inputFiles, String outputFile) {
        // 入力ストリーム
        InputStream is = null;

        // ZIP形式の出力ストリーム
        ZipOutputStream zos = null;

        // 入出力用のバッファを作成
        byte[] buf = new byte[1024];

        // ZipOutputStreamオブジェクトの作成
        try {
            zos = new ZipOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            for (int i = 0; i < inputFiles.size(); i++) {
                // 入力ストリームのオブジェクトを作成
                is = new FileInputStream((String) inputFiles.get(i));

                // Setting Filename
                String filename = imagenamearray.get(i);
                // String filename = String.format("img_%02d.jpg", i);

                // ZIPエントリを作成
                ZipEntry ze = new ZipEntry(filename);

                // 作成したZIPエントリを登録
                zos.putNextEntry(ze);

                // 入力ストリームからZIP形式の出力ストリームへ書き出す
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }

                // 入力ストリームを閉じる
                is.close();

                // エントリをクローズする
                zos.closeEntry();
            }

            // 出力ストリームを閉じる
            zos.close();
            textView.setText("圧縮＆保存完了");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //DB(Room)用


}

