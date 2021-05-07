package com.example.TATP_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
    //public TextView textView = findViewById(R.id.textView);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        Intent intent = this.getIntent();
        // = intent.getStringExtra("");
        // = intent.getBooleanExtra("",);

        MainActivity mainActivity = new MainActivity();
        String tsk_filename = mainActivity.ftext;
        Boolean rensyuuflg = mainActivity.rensyuflg;

///35タスクが終了し画面が遷移したら、画像フォルダを圧縮し、データを削除//

            // ZIP化実施フォルダを取得
            //File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + tsk_filename);

            //final File downloadDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + tsk_filename);

            String in = /*Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS+"/" +*/"/sdcard/Download/"+ tsk_filename;
            String out = /*Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS+"/" + */"/sdcard/Download/"+tsk_filename+".zip";

//圧縮する対象のファイル
            File[] files = new File(in).listFiles();

            ZipParameters params = new ZipParameters();
//圧縮アルゴリズムはDEFLATE
            params.setCompressionMethod(CompressionMethod.DEFLATE);
//圧縮レベルは高速
            params.setCompressionLevel(CompressionLevel.FAST);

//ファイルが存在しなければ新規、存在すれば追加となる
            ZipFile zip = new ZipFile(out);
            try {
                for (File f : files) {
                    System.out.println(f.getPath());

                    if (f.isDirectory()) {
                        //addFolder は配下の階層ごと追加する
                        zip.addFolder(f, params);
                    } else {
                        //addFile はファイル単体を追加する
                        zip.addFile(f, params);
                    }
                }
            } catch (IOException e) {
                //エラー処理（Zipファイルをを削除するなど）
            }


        Button bkbutton = findViewById(R.id.b);
        bkbutton.setOnClickListener(v -> {
            Intent restartintent = new Intent(getApplication(), StartActivity.class);
            startActivity(restartintent);
                }
        );
    }


}